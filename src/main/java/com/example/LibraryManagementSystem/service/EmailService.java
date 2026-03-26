package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MM yyyy");

    //Send welcome email to new use
    public void sendWelcomeEmail(Users users, Member member){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(users.getEmail());
            message.setSubject("Welcome to Library Management System!");

            String text = String.format(
                    "Hello %s,\n\n" +
                            "Welcome to our Library Management System!\n\n" +
                            "Your account has been successfully created:\n" +
                            "• Username: %s\n" +
                            "• Email: %s\n" +
                            "• Membership Type: %s\n" +
                            "• Member Since: %s\n\n" +
                            "You can now:\n" +
                            "✓ Browse our extensive book collection\n" +
                            "✓ Borrow up to %d books at a time\n" +
                            "✓ Keep books for up to %d days\n\n" +
                            "Start exploring our collection today!\n\n" +
                            "Happy Reading!\n" +
                            "Library Management Team",
                    member.getName(),
                    users.getUsername(),
                    users.getEmail(),
                    member.getMembershipType(),
                    member.getMembershipDate().format(DATE_FORMATTER),
                    member.getMembershipType().getMaxBooksAllowed(),
                    member.getMembershipType().getBorrowPeriodDays()
            );

            message.setText(text);
            javaMailSender.send(message);
            log.info("Welcome email sent to: {}",users.getEmail());
        }catch (Exception e){
           log.error("Failed to send welcome email to {}", users.getEmail(),e);
        }
    }

    //Send overdue reminder email
    public void sendBorrowConfirmation(BorrowRecord record){
        try{
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(record.getMember().getEmail());
            message.setSubject("Book Borrowed Successfully!");

            String text = String.format(
                    "Hello %s,\n\n" +
                            "You have successfully borrowed a book!\n\n" +
                            "Book Details:\n" +
                            "• Title: %s\n" +
                            "• Author: %s\n" +
                            "• ISBN: %s\n\n" +
                            "Borrow Information:\n" +
                            "• Borrowed On: %s\n" +
                            "• Due Date: %s\n" +
                            "• Days to Return: %d days\n\n" +
                            "⚠️ Important:\n" +
                            "• Please return the book by the due date to avoid late fees\n" +
                            "• Late fee: ₹1 per day (max ₹100)\n\n" +
                            "Enjoy your reading!\n\n" +
                            "Best regards,\n" +
                            "Library Management Team",
                    record.getMember().getName(),
                    record.getBook().getTitle(),
                    record.getBook().getAuthor().getName(),
                    record.getBook().getIsBn(),
                    record.getBorrowDate().format(DATE_FORMATTER),
                    record.getDueDate().format(DATE_FORMATTER),
                    record.getMember().getMembershipType().getBorrowPeriodDays()
            );

            message.setText(text);
            javaMailSender.send(message);
            log.info("Borrow confirmation sent to {}", record.getMember().getEmail());
        } catch (Exception e) {
            log.error("Failed to send Borrow confirmation", e);
        }
    }


    public void sendOverdueReminder(BorrowRecord record) {

        try {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    record.getDueDate(),
                    LocalDate.now()
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(record.getMember().getEmail());
            message.setSubject("Overdue Book Reminder - Please Return Soon");

            String text = String.format(
                    "Hello %s,\n\n" +
                            "This is a reminder that you have an overdue book.\n\n" +
                            "Book Details:\n" +
                            "• Title: %s\n" +
                            "• Author: %s\n" +
                            "• ISBN: %s\n\n" +
                            "Due Date: %s (%d days ago)\n" +
                            "Current Late Fee: ₹%.2f\n\n" +
                            "⚠️ IMPORTANT:\n" +
                            "• Please return the book as soon as possible\n" +
                            "• Late fees continue to accumulate at ₹1/day (max ₹100)\n" +
                            "• Your borrowing privileges may be suspended if not returned soon\n\n" +
                            "To avoid further charges, please return the book to the library today.\n\n" +
                            "If you have any questions, please contact us.\n\n" +
                            "Thank you,\n" +
                            "Library Management Team",
                    record.getMember().getName(),
                    record.getBook().getTitle(),
                    record.getBook().getAuthor().getName(),
                    record.getBook().getIsBn(),
                    record.getDueDate().format(DATE_FORMATTER),
                    daysOverdue,
                    record.getLateFee()
            );

            message.setText(text);
            javaMailSender.send(message);
            log.info("Overdue reminder sent to: {} for book: {}",
                    record.getMember().getEmail(), record.getBook().getTitle());
        } catch (Exception e) {
           log.error("Failed to send overdue reminder",e);
        }
    }

    public void sendReturnConfirmation(BorrowRecord record){
        try{

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(record.getMember().getEmail());
            message.setSubject("Book Returned Successfully!");

            boolean hasLateFee = record.getLateFee().doubleValue() > 0;

            String text = String.format(
                    "Hello %s,\n\n" +
                            "Thank you for returning your book!\n\n" +
                            "Book Details:\n" +
                            "• Title: %s\n" +
                            "• Author: %s\n\n" +
                            "Return Information:\n" +
                            "• Borrowed On: %s\n" +
                            "• Due Date: %s\n" +
                            "• Returned On: %s\n" +
                            "%s\n" +
                            "%s" +
                            "Thank you for using our library!\n" +
                            "Feel free to borrow more books anytime.\n\n" +
                            "Happy Reading!\n" +
                            "Library Management Team",
                    record.getMember().getName(),
                    record.getBook().getTitle(),
                    record.getBook().getAuthor().getName(),
                    record.getBorrowDate().format(DATE_FORMATTER),
                    record.getDueDate().format(DATE_FORMATTER),
                    record.getReturnDate().format(DATE_FORMATTER),
                    hasLateFee ? String.format("• Late Fee: ₹%.2f\n", record.getLateFee()) : "• Returned On Time! 🎉\n",
                    hasLateFee ? "\n⚠️ Please pay the late fee at the library counter.\n\n" : "\n"
            );
            message.setText(text);
            javaMailSender.send(message);
            log.info("Return confirmation sent to: {}", record.getMember().getEmail());
        }catch (Exception e){
            log.error("Failed to send return confirmation", e);
        }
    }
}
