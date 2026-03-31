//package com.kedarnath.service;
//
//import com.kedarnath.dto.*;
//import com.kedarnath.model.*;
//import com.kedarnath.repository.*;
//
//import com.razorpay.RazorpayClient;
//import com.razorpay.Order;
//import com.razorpay.Utils;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.json.JSONObject;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final CampRepository campRepository;
//    private final CampAllocationRepository allocationRepository;
//    private final PaymentRepository paymentRepository;
//    private final CampAllotmentService allotmentService;
//    private final EmailService emailService;
//
//    @Value("${razorpay.key.id}")    private String razorpayKeyId;
//    @Value("${razorpay.key.secret}") private String razorpayKeySecret;
//    @Value("${app.advance.percentage:30}") private int advancePercentage;
//
//    // ─── Preview allotment before booking ──────────────────────────────────────
//    public AllotmentPreviewResponse previewAllotment(int groupSize, String checkIn, String checkOut) {
//        var result = allotmentService.findBestAllotment(groupSize);
//        var resp = new AllotmentPreviewResponse();
//        resp.setAvailable(result.success());
//        resp.setMessage(result.message());
//
//        if (result.success()) {
//            long nights = ChronoUnit.DAYS.between(
//                java.time.LocalDate.parse(checkIn), java.time.LocalDate.parse(checkOut));
//            double total = allotmentService.calculateTotal(result.allocations(), (int) nights);
//            double advance = total * advancePercentage / 100.0;
//            resp.setEstimatedTotal(total);
//            resp.setEstimatedAdvance(advance);
//
//            List<CampAvailabilityResponse> allocDetails = result.allocations().entrySet().stream()
//                .map(e -> {
//                    var c = new CampAvailabilityResponse();
//                    c.setId(e.getKey().getId());
//                    c.setName(e.getKey().getName());
//                    c.setCapacity(e.getKey().getCapacity());
//                    c.setPricePerNight(e.getKey().getPricePerNight());
//                    return c;
//                }).collect(Collectors.toList());
//            resp.setSuggestedAllocations(allocDetails);
//        }
//        return resp;
//    }
//
//    // ─── Create booking + Razorpay order ───────────────────────────────────────
//    @Transactional
//    public PaymentOrderResponse createBooking(BookingRequest req) throws Exception {
//        // Validate dates
//        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
//            throw new IllegalArgumentException("Check-out must be after check-in.");
//        }
//
//        // Find best camp combination
//        var allotment = allotmentService.findBestAllotment(req.getGroupSize());
//        if (!allotment.success()) {
//            throw new IllegalStateException(allotment.message());
//        }
//
//        long nights = ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
//        double total = allotmentService.calculateTotal(allotment.allocations(), (int) nights);
//        double advance = Math.ceil(total * advancePercentage / 100.0);
//
//        // Create booking record
//        Booking booking = new Booking();
//        booking.setCustomerName(req.getCustomerName());
//        booking.setCustomerEmail(req.getCustomerEmail());
//        booking.setCustomerPhone(req.getCustomerPhone());
//        booking.setGroupSize(req.getGroupSize());
//        booking.setCheckIn(req.getCheckIn());
//        booking.setCheckOut(req.getCheckOut());
//        booking.setTotalAmount(total);
//        booking.setAdvanceAmount(advance);
//        booking.setSpecialRequests(req.getSpecialRequests());
//        booking.setStatus(Booking.BookingStatus.PAYMENT_PENDING);
//        booking = bookingRepository.save(booking);
//
//        // Save camp allocations and temporarily hold camps
//        for (Map.Entry<Camp, Integer> entry : allotment.allocations().entrySet()) {
//            Camp camp = entry.getKey();
//            int qty = entry.getValue();
//            CampAllocation alloc = new CampAllocation(booking, camp, qty);
//            allocationRepository.save(alloc);
//            camp.setBookedCount(camp.getBookedCount() + qty);
//            campRepository.save(camp);
//        }
//
//        // Create Razorpay order
//        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
//        JSONObject orderReq = new JSONObject();
//        orderReq.put("amount", (int)(advance * 100)); // paise
//        orderReq.put("currency", "INR");
//        orderReq.put("receipt", booking.getId().toString().substring(0, 20));
//        Order order = client.orders.create(orderReq);
//
//        // Save payment record
//        com.kedarnath.model.Payment payment = new com.kedarnath.model.Payment();
//        payment.setBooking(booking);
//        payment.setRazorpayOrderId(order.get("id"));
//        payment.setAmount(advance);
//        paymentRepository.save(payment);
//
//        // Build response
//        PaymentOrderResponse resp = new PaymentOrderResponse();
//        resp.setRazorpayOrderId(order.get("id"));
//        resp.setAmount(advance);
//        resp.setCurrency("INR");
//        resp.setKeyId(razorpayKeyId);
//        resp.setBookingId(booking.getId());
//        resp.setCustomerName(req.getCustomerName());
//        resp.setCustomerEmail(req.getCustomerEmail());
//        resp.setCustomerPhone(req.getCustomerPhone());
//        return resp;
//    }
//
//    // ─── Verify payment + confirm ───────────────────────────────────────────────
//    @Transactional
//    public BookingResponse verifyPayment(PaymentVerifyRequest req) throws Exception {
//        // Verify HMAC signature
//        JSONObject params = new JSONObject();
//        params.put("razorpay_order_id", req.getRazorpayOrderId());
//        params.put("razorpay_payment_id", req.getRazorpayPaymentId());
//        params.put("razorpay_signature", req.getRazorpaySignature());
//        boolean valid = Utils.verifyPaymentSignature(params, razorpayKeySecret);
//        if (!valid) throw new SecurityException("Payment signature verification failed.");
//
//        // Update payment record
//        com.kedarnath.model.Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
//                .orElseThrow(() -> new NoSuchElementException("Payment order not found."));
//        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
//        payment.setRazorpaySignature(req.getRazorpaySignature());
//        payment.setStatus(com.kedarnath.model.Payment.PaymentStatus.SUCCESS);
//        paymentRepository.save(payment);
//
//        // Confirm booking
//        Booking booking = payment.getBooking();
//        booking.setStatus(Booking.BookingStatus.CONFIRMED);
//        bookingRepository.save(booking);
//
//        // Send confirmation email (async)
//        try { emailService.sendBookingConfirmation(booking); }
//        catch (Exception e) { log.warn("Email send failed: {}", e.getMessage()); }
//
//        return toBookingResponse(booking);
//    }
//
//    // ─── Get all camps availability ─────────────────────────────────────────────
//    public List<CampAvailabilityResponse> getAllCampsAvailability() {
//        return campRepository.findAllByOrderByCapacityAsc().stream()
//                .map(this::toCampResponse)
//                .collect(Collectors.toList());
//    }
//
//    // ─── Admin: get all bookings ─────────────────────────────────────────────────
//    public List<BookingResponse> getAllBookings() {
//        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
//                .map(this::toBookingResponse)
//                .collect(Collectors.toList());
//    }
//
//    // ─── Admin: update booking status ───────────────────────────────────────────
//    @Transactional
//    public BookingResponse updateBookingStatus(UUID bookingId, Booking.BookingStatus newStatus) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new NoSuchElementException("Booking not found."));
//
//        // If cancelling, release camp slots
//        if (newStatus == Booking.BookingStatus.CANCELLED
//                && booking.getStatus() != Booking.BookingStatus.CANCELLED) {
//            for (CampAllocation alloc : booking.getCampAllocations()) {
//                Camp camp = alloc.getCamp();
//                camp.setBookedCount(Math.max(0, camp.getBookedCount() - alloc.getQuantity()));
//                campRepository.save(camp);
//            }
//        }
//        booking.setStatus(newStatus);
//        return toBookingResponse(bookingRepository.save(booking));
//    }
//
//    // ─── Mappers ─────────────────────────────────────────────────────────────────
//    private CampAvailabilityResponse toCampResponse(Camp c) {
//        var r = new CampAvailabilityResponse();
//        r.setId(c.getId()); r.setName(c.getName());
//        r.setCapacity(c.getCapacity()); r.setTotalCount(c.getTotalCount());
//        r.setBookedCount(c.getBookedCount()); r.setAvailableCount(c.getAvailableCount());
//        r.setPricePerNight(c.getPricePerNight()); r.setDescription(c.getDescription());
//        return r;
//    }
//
//    BookingResponse toBookingResponse(Booking b) {
//        var r = new BookingResponse();
//        r.setId(b.getId()); r.setCustomerName(b.getCustomerName());
//        r.setCustomerEmail(b.getCustomerEmail()); r.setCustomerPhone(b.getCustomerPhone());
//        r.setGroupSize(b.getGroupSize()); r.setCheckIn(b.getCheckIn());
//        r.setCheckOut(b.getCheckOut());
//        r.setTotalNights((int) ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut()));
//        r.setTotalAmount(b.getTotalAmount()); r.setAdvanceAmount(b.getAdvanceAmount());
//        r.setStatus(b.getStatus().name());
//        if (b.getCampAllocations() != null) {
//            r.setAllocations(b.getCampAllocations().stream().map(a -> {
//                var d = new BookingResponse.AllocationDetail();
//                d.setCampName(a.getCamp().getName()); d.setCapacity(a.getCamp().getCapacity());
//                d.setQuantity(a.getQuantity()); d.setPricePerNight(a.getCamp().getPricePerNight());
//                return d;
//            }).collect(Collectors.toList()));
//        }
//        if (b.getPayment() != null) r.setRazorpayOrderId(b.getPayment().getRazorpayOrderId());
//        return r;
//    }
//}


package com.kedarnath.service;

import com.kedarnath.dto.*;
import com.kedarnath.model.*;
import com.kedarnath.repository.*;

import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CampRepository campRepository;
    private final CampAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;
    private final CampAllotmentService allotmentService;
    private final EmailService emailService;

    @Value("${razorpay.key.id}")     private String razorpayKeyId;
    @Value("${razorpay.key.secret}") private String razorpayKeySecret;
    @Value("${app.advance.percentage:30}") private int advancePercentage;

    // ─── Preview allotment before booking ──────────────────────────────────────
    public AllotmentPreviewResponse previewAllotment(int groupSize, String checkIn, String checkOut) {
        var result = allotmentService.findBestAllotment(groupSize);
        var resp = new AllotmentPreviewResponse();
        resp.setAvailable(result.success());
        resp.setMessage(result.message());

        if (result.success()) {
            long nights = ChronoUnit.DAYS.between(
                    java.time.LocalDate.parse(checkIn), java.time.LocalDate.parse(checkOut));
            double total = allotmentService.calculateTotal(result.allocations(), (int) nights);
            double advance = total * advancePercentage / 100.0;
            resp.setEstimatedTotal(total);
            resp.setEstimatedAdvance(advance);

            List<CampAvailabilityResponse> allocDetails = result.allocations().entrySet().stream()
                    .map(e -> {
                        var c = new CampAvailabilityResponse();
                        c.setId(e.getKey().getId());
                        c.setName(e.getKey().getName());
                        c.setCapacity(e.getKey().getCapacity());
                        c.setPricePerNight(e.getKey().getPricePerNight());
                        return c;
                    }).collect(Collectors.toList());
            resp.setSuggestedAllocations(allocDetails);
        }
        return resp;
    }

    // ─── Create booking + Razorpay order ───────────────────────────────────────
    @Transactional
    public PaymentOrderResponse createBooking(BookingRequest req) throws Exception {

        // FIX 1: Validate dates
        if (req.getCheckIn() == null || req.getCheckOut() == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }

        // Find best camp combination
        var allotment = allotmentService.findBestAllotment(req.getGroupSize());
        if (!allotment.success()) {
            throw new IllegalStateException(allotment.message());
        }

        long nights = ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        double total = allotmentService.calculateTotal(allotment.allocations(), (int) nights);

        // FIX 2: advance must be a whole number (Razorpay needs integer paise)
        long advance = (long) Math.ceil(total * advancePercentage / 100.0);

        // Create booking record
        Booking booking = new Booking();
        booking.setCustomerName(req.getCustomerName());
        booking.setCustomerEmail(req.getCustomerEmail());
        booking.setCustomerPhone(req.getCustomerPhone());
        booking.setGroupSize(req.getGroupSize());
        booking.setCheckIn(req.getCheckIn());
        booking.setCheckOut(req.getCheckOut());
        booking.setTotalAmount(total);
        booking.setAdvanceAmount((double) advance);
        booking.setSpecialRequests(req.getSpecialRequests());
        booking.setStatus(Booking.BookingStatus.PAYMENT_PENDING);
        booking = bookingRepository.save(booking);

        // Save camp allocations and temporarily hold camps
        for (Map.Entry<Camp, Integer> entry : allotment.allocations().entrySet()) {
            Camp camp = entry.getKey();
            int qty = entry.getValue();
            CampAllocation alloc = new CampAllocation(booking, camp, qty);
            allocationRepository.save(alloc);
            camp.setBookedCount(camp.getBookedCount() + qty);
            campRepository.save(camp);
        }

        // FIX 3: receipt must be max 40 chars and only alphanumeric/_/-
        // UUID format: 550e8400-e29b-41d4-a716-446655440000 (36 chars)
        // Remove dashes, take first 20 chars, prefix with "rcpt_"
        String receiptId = "rcpt_" + booking.getId().toString().replace("-", "").substring(0, 20);

        // FIX 4: amount in paise must be a long, not int (avoids overflow for large amounts)
        long amountInPaise = advance * 100L;

        log.info("Creating Razorpay order: receipt={}, amount={} paise", receiptId, amountInPaise);

        // Create Razorpay order
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderReq = new JSONObject();
        orderReq.put("amount", amountInPaise);
        orderReq.put("currency", "INR");
        orderReq.put("receipt", receiptId);
        Order order = client.orders.create(orderReq);

       //vi log.info("Razorpay order created: {}", order.get("id"));

        // Save payment record
        com.kedarnath.model.Payment payment = new com.kedarnath.model.Payment();
        payment.setBooking(booking);
        payment.setRazorpayOrderId(order.get("id"));
        payment.setAmount((double) advance);
        paymentRepository.save(payment);

        // Build response
        PaymentOrderResponse resp = new PaymentOrderResponse();
        resp.setRazorpayOrderId(order.get("id"));
        resp.setAmount((double) advance);
        resp.setCurrency("INR");
        resp.setKeyId(razorpayKeyId);
        resp.setBookingId(booking.getId());
        resp.setCustomerName(req.getCustomerName());
        resp.setCustomerEmail(req.getCustomerEmail());
        resp.setCustomerPhone(req.getCustomerPhone());
        return resp;
    }

    // ─── Verify payment + confirm ───────────────────────────────────────────────
    @Transactional
    public BookingResponse verifyPayment(PaymentVerifyRequest req) throws Exception {

        // FIX 5: Validate all three fields are present before verification
        if (req.getRazorpayOrderId() == null
                || req.getRazorpayPaymentId() == null
                || req.getRazorpaySignature() == null) {
            throw new IllegalArgumentException("Missing payment verification fields.");
        }

        // Verify HMAC signature
        JSONObject params = new JSONObject();
        params.put("razorpay_order_id",   req.getRazorpayOrderId());
        params.put("razorpay_payment_id", req.getRazorpayPaymentId());
        params.put("razorpay_signature",  req.getRazorpaySignature());
        boolean valid = Utils.verifyPaymentSignature(params, razorpayKeySecret);
        if (!valid) throw new SecurityException("Payment signature verification failed.");

        // Update payment record
        com.kedarnath.model.Payment payment = paymentRepository
                .findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new NoSuchElementException("Payment order not found."));

        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setStatus(com.kedarnath.model.Payment.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Confirm booking
        Booking booking = payment.getBooking();
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Send confirmation email (async, non-blocking)
        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            log.warn("Email send failed: {}", e.getMessage());
        }

        return toBookingResponse(booking);
    }

    // ─── Get all camps availability ─────────────────────────────────────────────
    public List<CampAvailabilityResponse> getAllCampsAvailability() {
        return campRepository.findAllByOrderByCapacityAsc().stream()
                .map(this::toCampResponse)
                .collect(Collectors.toList());
    }

    // ─── Admin: get all bookings ─────────────────────────────────────────────────
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    // ─── Admin: update booking status ───────────────────────────────────────────
    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, Booking.BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found."));

        if (newStatus == Booking.BookingStatus.CANCELLED
                && booking.getStatus() != Booking.BookingStatus.CANCELLED) {
            for (CampAllocation alloc : booking.getCampAllocations()) {
                Camp camp = alloc.getCamp();
                camp.setBookedCount(Math.max(0, camp.getBookedCount() - alloc.getQuantity()));
                campRepository.save(camp);
            }
        }
        booking.setStatus(newStatus);
        return toBookingResponse(bookingRepository.save(booking));
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────────
    private CampAvailabilityResponse toCampResponse(Camp c) {
        var r = new CampAvailabilityResponse();
        r.setId(c.getId());            r.setName(c.getName());
        r.setCapacity(c.getCapacity()); r.setTotalCount(c.getTotalCount());
        r.setBookedCount(c.getBookedCount()); r.setAvailableCount(c.getAvailableCount());
        r.setPricePerNight(c.getPricePerNight()); r.setDescription(c.getDescription());
        return r;
    }

    BookingResponse toBookingResponse(Booking b) {
        var r = new BookingResponse();
        r.setId(b.getId());
        r.setCustomerName(b.getCustomerName());
        r.setCustomerEmail(b.getCustomerEmail());
        r.setCustomerPhone(b.getCustomerPhone());
        r.setGroupSize(b.getGroupSize());
        r.setCheckIn(b.getCheckIn());
        r.setCheckOut(b.getCheckOut());
        r.setTotalNights((int) ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut()));
        r.setTotalAmount(b.getTotalAmount());
        r.setAdvanceAmount(b.getAdvanceAmount());
        r.setStatus(b.getStatus().name());

        if (b.getCampAllocations() != null) {
            r.setAllocations(b.getCampAllocations().stream().map(a -> {
                var d = new BookingResponse.AllocationDetail();
                d.setCampName(a.getCamp().getName());
                d.setCapacity(a.getCamp().getCapacity());
                d.setQuantity(a.getQuantity());
                d.setPricePerNight(a.getCamp().getPricePerNight());
                return d;
            }).collect(Collectors.toList()));
        }

        // FIX 6: safely get payment info — payment may be null
        if (b.getPayment() != null) {
            r.setRazorpayOrderId(b.getPayment().getRazorpayOrderId());
        }
        return r;
    }
}
