package com.fayzo.fleetpulse.payroll;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Payroll calculation. Assumptions (documented, adjust to local labour rules before production use):
 *  - Working days = Monday to Friday of the month; a standard day is 8 hours.
 *  - Absent days (not present, not on APPROVED leave) are deducted at monthlySalary / workingDays.
 *  - Overtime = hours beyond 8 per attended day x overtimeRate.
 *  - Open salary advances are recovered in full from the payslip, never below zero net pay.
 *  - Work-based pay = APPROVED work records of the month that are not yet PAID. Pending ones are never paid.
 * Bonus and manual deductions can be edited on a DRAFT payslip before finalising.
 */
@Service
@Transactional
public class PayrollService {
    private static final BigDecimal STANDARD_HOURS = BigDecimal.valueOf(8);
    private final JdbcTemplate jdbc;
    public PayrollService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> generate(String periodMonth) {
        long c = TenantContext.require();
        YearMonth ym;
        try { ym = YearMonth.parse(periodMonth); } catch (Exception e) { throw ApiException.badRequest("BAD_PERIOD", "periodMonth must be YYYY-MM"); }
        LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();

        List<Long> runIds = jdbc.queryForList("SELECT id FROM payroll_runs WHERE company_id=? AND period_month=?", Long.class, c, periodMonth);
        long runId;
        if (runIds.isEmpty()) {
            jdbc.update("INSERT INTO payroll_runs(company_id,period_month,status,total_net) VALUES (?,?,'DRAFT',0)", c, periodMonth);
            runId = jdbc.queryForObject("SELECT id FROM payroll_runs WHERE company_id=? AND period_month=?", Long.class, c, periodMonth);
        } else {
            runId = runIds.get(0);
            String st = jdbc.queryForObject("SELECT status FROM payroll_runs WHERE id=?", String.class, runId);
            if (!"DRAFT".equals(st)) throw ApiException.conflict("RUN_LOCKED", "This payroll run is already " + st);
        }

        int workingDays = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) workingDays++;

        List<Map<String, Object>> emps = jdbc.queryForList("""
            SELECT e.id, ss.model, ss.monthly_salary, ss.overtime_rate FROM employees e
            JOIN salary_structures ss ON ss.employee_id=e.id AND ss.company_id=e.company_id
            WHERE e.company_id=? AND e.status='ACTIVE'""", c);
        BigDecimal totalNet = BigDecimal.ZERO;
        int generated = 0;
        for (Map<String, Object> e : emps) {
            long empId = ((Number) e.get("id")).longValue();
            String model = (String) e.get("model");
            List<Map<String, Object>> existing = jdbc.queryForList("SELECT id, status, bonus, deductions FROM payslips WHERE company_id=? AND employee_id=? AND period_month=?", c, empId, periodMonth);
            if (!existing.isEmpty() && !"DRAFT".equals(existing.get(0).get("status"))) continue;
            BigDecimal bonus = existing.isEmpty() || existing.get(0).get("bonus") == null ? BigDecimal.ZERO : (BigDecimal) existing.get(0).get("bonus");
            BigDecimal manualDeductions = existing.isEmpty() || existing.get(0).get("deductions") == null ? BigDecimal.ZERO : (BigDecimal) existing.get(0).get("deductions");

            BigDecimal basic = BigDecimal.ZERO, overtime = BigDecimal.ZERO, workEarnings = BigDecimal.ZERO, absenceDeduction = BigDecimal.ZERO;
            int present = 0, leave = 0;
            if ("FIXED".equals(model)) {
                BigDecimal monthly = nz((BigDecimal) e.get("monthly_salary"));
                basic = monthly;
                present = count("SELECT COUNT(*) FROM attendance WHERE company_id=? AND employee_id=? AND work_date BETWEEN ? AND ? AND status IN ('PRESENT','LATE')", c, empId, from, to);
                leave = leaveWorkingDays(c, empId, from, to);
                int absent = Math.max(0, workingDays - present - leave);
                if (workingDays > 0) absenceDeduction = monthly.divide(BigDecimal.valueOf(workingDays), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(absent));
                BigDecimal otHours = jdbc.queryForObject("""
                    SELECT COALESCE(SUM(GREATEST(0, TIMESTAMPDIFF(MINUTE, check_in, check_out)/60 - ?)),0) FROM attendance
                    WHERE company_id=? AND employee_id=? AND work_date BETWEEN ? AND ? AND check_in IS NOT NULL AND check_out IS NOT NULL""", BigDecimal.class, STANDARD_HOURS, c, empId, from, to);
                overtime = nz(otHours).multiply(nz((BigDecimal) e.get("overtime_rate")));
            } else {
                workEarnings = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM work_records WHERE company_id=? AND employee_id=? AND status='APPROVED' AND work_date BETWEEN ? AND ?", BigDecimal.class, c, empId, from, to);
                present = count("SELECT COUNT(*) FROM attendance WHERE company_id=? AND employee_id=? AND work_date BETWEEN ? AND ? AND status IN ('PRESENT','LATE')", c, empId, from, to);
            }
            BigDecimal gross = basic.add(overtime).add(bonus).add(workEarnings);
            BigDecimal deductions = manualDeductions.add(absenceDeduction).setScale(2, RoundingMode.HALF_UP);
            BigDecimal beforeAdvances = gross.subtract(deductions).max(BigDecimal.ZERO);
            BigDecimal outstanding = jdbc.queryForObject("SELECT COALESCE(SUM(amount-repaid),0) FROM salary_advances WHERE company_id=? AND employee_id=? AND status='OPEN'", BigDecimal.class, c, empId);
            BigDecimal advances = nz(outstanding).min(beforeAdvances);
            BigDecimal net = beforeAdvances.subtract(advances).setScale(2, RoundingMode.HALF_UP);

            if (existing.isEmpty()) {
                jdbc.update("""
                    INSERT INTO payslips(company_id,payroll_run_id,employee_id,period_month,model,basic,overtime,bonus,deductions,advances,work_earnings,net_salary,working_days,present_days,leave_days,status)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT')""", c, runId, empId, periodMonth, model, basic, overtime.setScale(2, RoundingMode.HALF_UP), bonus, deductions, advances, workEarnings, net, workingDays, present, leave);
            } else {
                jdbc.update("""
                    UPDATE payslips SET payroll_run_id=?, model=?, basic=?, overtime=?, deductions=?, advances=?, work_earnings=?, net_salary=?, working_days=?, present_days=?, leave_days=?
                    WHERE id=? AND company_id=?""", runId, model, basic, overtime.setScale(2, RoundingMode.HALF_UP), deductions, advances, workEarnings, net, workingDays, present, leave, existing.get(0).get("id"), c);
            }
            totalNet = totalNet.add(net);
            generated++;
        }
        jdbc.update("UPDATE payroll_runs SET total_net=? WHERE id=? AND company_id=?", totalNet, runId, c);
        return Map.of("runId", runId, "periodMonth", periodMonth, "payslips", generated, "totalNet", totalNet);
    }

    /** DRAFT -> FINALIZED: payslips become visible to employees and advances are recovered. */
    public Map<String, Object> finalizeRun(long runId) {
        long c = TenantContext.require();
        requireRun(runId, "DRAFT");
        List<Map<String, Object>> slips = jdbc.queryForList("SELECT employee_id, advances FROM payslips WHERE company_id=? AND payroll_run_id=? AND status='DRAFT'", c, runId);
        for (Map<String, Object> s : slips) recoverAdvances(c, ((Number) s.get("employee_id")).longValue(), nz((BigDecimal) s.get("advances")));
        jdbc.update("UPDATE payslips SET status='FINALIZED' WHERE company_id=? AND payroll_run_id=? AND status='DRAFT'", c, runId);
        jdbc.update("UPDATE payroll_runs SET status='FINALIZED' WHERE id=? AND company_id=?", runId, c);
        return Map.of("runId", runId, "status", "FINALIZED", "payslips", slips.size());
    }

    /** FINALIZED -> PAID: marks included work records as paid and books one SALARY expense per payslip. */
    public Map<String, Object> markPaid(long runId) {
        long c = TenantContext.require();
        Map<String, Object> run = requireRun(runId, "FINALIZED");
        String period = (String) run.get("period_month");
        YearMonth ym = YearMonth.parse(period);
        jdbc.update("""
            UPDATE work_records w JOIN payslips p ON p.employee_id=w.employee_id AND p.company_id=w.company_id
            SET w.status='PAID' WHERE w.company_id=? AND p.payroll_run_id=? AND w.status='APPROVED' AND w.work_date BETWEEN ? AND ?""", c, runId, ym.atDay(1), ym.atEndOfMonth());
        jdbc.update("""
            INSERT INTO expenses(company_id,category,amount,expense_date,description)
            SELECT company_id,'SALARY',net_salary,CURDATE(),CONCAT('Salary ',period_month,' employee #',employee_id) FROM payslips
            WHERE company_id=? AND payroll_run_id=? AND status='FINALIZED' AND net_salary > 0""", c, runId);
        jdbc.update("UPDATE payslips SET status='PAID', paid_on=CURDATE() WHERE company_id=? AND payroll_run_id=? AND status='FINALIZED'", c, runId);
        jdbc.update("UPDATE payroll_runs SET status='PAID' WHERE id=? AND company_id=?", runId, c);
        return Map.of("runId", runId, "status", "PAID");
    }

    public void decideWork(long workRecordId, boolean approve, long approverUserId) {
        long c = TenantContext.require();
        int n = jdbc.update("UPDATE work_records SET status=?, approved_by=? WHERE id=? AND company_id=? AND status='PENDING'", approve ? "APPROVED" : "REJECTED", approverUserId, workRecordId, c);
        if (n == 0) throw ApiException.badRequest("NOT_PENDING", "Work record not found or already decided");
    }

    private void recoverAdvances(long c, long empId, BigDecimal amount) {
        BigDecimal left = amount;
        for (Map<String, Object> a : jdbc.queryForList("SELECT id, amount, repaid FROM salary_advances WHERE company_id=? AND employee_id=? AND status='OPEN' ORDER BY advance_date, id", c, empId)) {
            if (left.signum() <= 0) break;
            BigDecimal remaining = ((BigDecimal) a.get("amount")).subtract(nz((BigDecimal) a.get("repaid")));
            BigDecimal take = remaining.min(left);
            BigDecimal newRepaid = nz((BigDecimal) a.get("repaid")).add(take);
            jdbc.update("UPDATE salary_advances SET repaid=?, status=? WHERE id=? AND company_id=?", newRepaid, newRepaid.compareTo((BigDecimal) a.get("amount")) >= 0 ? "CLOSED" : "OPEN", a.get("id"), c);
            left = left.subtract(take);
        }
    }

    private int leaveWorkingDays(long c, long empId, LocalDate from, LocalDate to) {
        int days = 0;
        for (Map<String, Object> l : jdbc.queryForList("SELECT from_date, to_date FROM leave_requests WHERE company_id=? AND employee_id=? AND status='APPROVED' AND from_date <= ? AND to_date >= ?", c, empId, to, from)) {
            LocalDate s = ((java.sql.Date) l.get("from_date")).toLocalDate().isBefore(from) ? from : ((java.sql.Date) l.get("from_date")).toLocalDate();
            LocalDate e = ((java.sql.Date) l.get("to_date")).toLocalDate().isAfter(to) ? to : ((java.sql.Date) l.get("to_date")).toLocalDate();
            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) days++;
        }
        return days;
    }

    private Map<String, Object> requireRun(long id, String expected) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT id, status, period_month FROM payroll_runs WHERE id=? AND company_id=?", id, TenantContext.require());
        if (r.isEmpty()) throw ApiException.notFound("Payroll run not found");
        if (!expected.equals(r.get(0).get("status"))) throw ApiException.conflict("BAD_RUN_STATE", "Payroll run must be " + expected + " (is " + r.get(0).get("status") + ")");
        return r.get(0);
    }

    private int count(String sql, Object... a) { Integer n = jdbc.queryForObject(sql, Integer.class, a); return n == null ? 0 : n; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
