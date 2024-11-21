package com.example.payments.service;

import com.example.payments.dto.Paymentdto;
import com.example.payments.model.Payment;
import com.example.payments.repository.PaymentRepository;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    public Payment getPaymentById(String id) {
        return paymentRepository.findById(id).orElse(null);
    }

    public Payment initiatePayment(Paymentdto payment) {
        Payment p=Payment.builder()
                .amount(payment.getAmount())
                .username(payment.getUsername())
                .currency(payment.getCurrency())
                .poNumber(payment.getPoNumber())
                .invoiceNumber(payment.getInvoiceNumber())
                .targetBankAccount(payment.getTargetBankAccount())
                .tds(payment.getTds())
                .sourceBankAccount(payment.getSourceBankAccount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .items(payment.getItems())
                .quantity(payment.getQuantity())
                .build();
        return paymentRepository.save(p);
    }

    // Method to initiate a list of payments
    public List<Payment> initiatePayments(List<Paymentdto> payments) {
        List<Payment> paymentList = payments.stream().map(payment -> Payment.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .username(payment.getUsername())
                .poNumber(payment.getPoNumber())
                .invoiceNumber(payment.getInvoiceNumber())
                .targetBankAccount(payment.getTargetBankAccount())
                .tds(payment.getTds())
                .sourceBankAccount(payment.getSourceBankAccount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build()).collect(Collectors.toList());

        return paymentRepository.saveAll(paymentList);
    }

    // 1. Find pending payments
    public List<Payment> findPendingPayments() {
        return paymentRepository.findByStatus("PENDING");
    }

    // 2. Find total amount
    public Double getTotalAmount() {
        return paymentRepository.sumAllAmounts();
    }

    // 3. Find amount by invoice number
    public Double getAmountByInvoiceNumber(String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber);
        return payment != null ? payment.getAmount() : 0.0;
    }

    // 4. Find complete and pending payments by payment date
    public Map<String, List<Payment>> getPaymentsByStatusAndDate(String paymentDate) {
        Map<String, List<Payment>> paymentsByStatus = new HashMap<>();
        paymentsByStatus.put("completed", paymentRepository.findByPaymentDateAndStatus(paymentDate, "PAID"));
        paymentsByStatus.put("pending", paymentRepository.findByPaymentDateAndStatus(paymentDate, "PENDING"));
        return paymentsByStatus;
    }

    // 5. Edit payment
    public Payment editPayment(String id, Paymentdto paymentdto) {
        Optional<Payment> optionalPayment = paymentRepository.findById(id);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setAmount(paymentdto.getAmount());
            payment.setCurrency(paymentdto.getCurrency());
            payment.setUsername(paymentdto.getUsername());
            payment.setPoNumber(paymentdto.getPoNumber());
            payment.setInvoiceNumber(paymentdto.getInvoiceNumber());
            payment.setTargetBankAccount(paymentdto.getTargetBankAccount());
            payment.setSourceBankAccount(paymentdto.getSourceBankAccount());
            payment.setTds(paymentdto.getTds());
            payment.setStatus(paymentdto.getStatus());
            payment.setPaymentDate(paymentdto.getPaymentDate());
            return paymentRepository.save(payment);
        }
        throw new RuntimeException("Payment not found");
    }

    // 6. Delete payment
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    public void generateInvoicePDF(Payment invoice) throws JRException {
        // Load Jasper template
        JasperReport jasperReport = JasperCompileManager.compileReport("src/main/resources/invoices/invoice_template.jrxml");

        // Prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("amount", invoice.getAmount());
        parameters.put("username", invoice.getUsername());
        parameters.put("purchaseOrderNumber", invoice.getPoNumber());
        parameters.put("invoiceNumber", invoice.getInvoiceNumber());
        parameters.put("targetBankAccount", invoice.getTargetBankAccount());
        parameters.put("sourceBankAccount", invoice.getSourceBankAccount());
        parameters.put("tds", invoice.getTds());
        parameters.put("status", invoice.getStatus());
        parameters.put("paymentDate", invoice.getPaymentDate());
        parameters.put("item", invoice.getItems());
        parameters.put("quantity", invoice.getQuantity());

        // Fill the report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

        // Export to PDF
        JasperExportManager.exportReportToPdfFile(jasperPrint, "invoice_" + invoice.getInvoiceNumber() + ".pdf");
    }
}
