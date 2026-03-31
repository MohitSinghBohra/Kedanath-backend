package com.kedarnath.service;

import com.kedarnath.model.Booking;
import com.kedarnath.model.CampAllocation;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}") private String fromAddress;
    @Value("${app.mail.from-name}") private String fromName;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject("Booking Confirmed - Kedarnath Camps #" +
                              booking.getId().toString().substring(0, 8).toUpperCase());
            helper.setText(buildConfirmationHtml(booking), true);
            mailSender.send(msg);
            log.info("Confirmation email sent to {}", booking.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", booking.getCustomerEmail(), e.getMessage());
        }
    }

    private String buildConfirmationHtml(Booking b) {
        StringBuilder camps = new StringBuilder();
        if (b.getCampAllocations() != null) {
            for (CampAllocation a : b.getCampAllocations()) {
                camps.append("<tr><td>").append(a.getCamp().getName())
                     .append("</td><td>x").append(a.getQuantity())
                     .append("</td><td>₹").append(a.getCamp().getPricePerNight())
                     .append("/night</td></tr>");
            }
        }
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px">
              <div style="background:#1a472a;color:white;padding:20px;border-radius:8px;text-align:center">
                <h1 style="margin:0">Kedarnath Camps</h1>
                <p style="margin:5px 0">Booking Confirmed!</p>
              </div>
              <div style="padding:20px;border:1px solid #ddd;border-radius:8px;margin-top:10px">
                <h2 style="color:#1a472a">Hello, %s!</h2>
                <p>Your booking is confirmed. Here are your details:</p>
                <table style="width:100%%;border-collapse:collapse">
                  <tr><td><b>Booking ID:</b></td><td>%s</td></tr>
                  <tr><td><b>Check-in:</b></td><td>%s</td></tr>
                  <tr><td><b>Check-out:</b></td><td>%s</td></tr>
                  <tr><td><b>Group Size:</b></td><td>%d people</td></tr>
                  <tr><td><b>Advance Paid:</b></td><td>₹%.0f</td></tr>
                  <tr><td><b>Balance Due:</b></td><td>₹%.0f (pay at camp)</td></tr>
                </table>
                <h3 style="color:#1a472a">Allocated Camps:</h3>
                <table style="width:100%%;border-collapse:collapse;border:1px solid #ddd">
                  <tr style="background:#f0f0f0"><th>Camp</th><th>Qty</th><th>Rate</th></tr>
                  %s
                </table>
                <p style="margin-top:20px;color:#666;font-size:13px">
                  For any queries, contact us. See you at Kedarnath!
                </p>
              </div>
            </div>
            """.formatted(
                b.getCustomerName(),
                b.getId().toString().substring(0, 8).toUpperCase(),
                b.getCheckIn(), b.getCheckOut(),
                b.getGroupSize(), b.getAdvanceAmount(),
                b.getTotalAmount() - b.getAdvanceAmount(),
                camps.toString()
            );
    }
}
