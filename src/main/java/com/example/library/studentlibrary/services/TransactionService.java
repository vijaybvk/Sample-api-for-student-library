package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases
        String transactionId="";
        Optional<Book> book=bookRepository5.findById(bookId);
        if(book.isPresent() && book.get().isAvailable()){
            Optional<Card> card=cardRepository5.findById(cardId);
            if(card.isPresent() && card.get().getCardStatus()==CardStatus.ACTIVATED){
                if(card.get().getBooks().size()<max_allowed_books){
                    Transaction transaction=Transaction.builder().book(book.get()).card(card.get()).
                            transactionStatus(TransactionStatus.SUCCESSFUL).isIssueOperation(true).transactionDate(new Date()).build();
                    List<Book> books=card.get().getBooks();
                    books.add(book.get());
                    card.get(   ).setBooks(books);
                    cardRepository5.save(card.get());
                    List<Transaction> transactions=book.get().getTransactions();
                    transactions.add(transaction);
                    book.get().setAvailable(false);
                    book.get().setTransactions(transactions);
                    transactionId=transaction.getTransactionId();
                    bookRepository5.save(book.get());
                    transactionRepository5.save(transaction);
                }
                else{
                    Transaction transaction=Transaction.builder().transactionDate(new Date()).isIssueOperation(true).book(book.get())
                            .card(card.get()).transactionStatus(TransactionStatus.FAILED).build();
                    transactionRepository5.save(transaction);
                    try {
                        throw new Exception("Book limit has reached for this card");
                    }catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                }
            }
            else{
                Transaction transaction=Transaction.builder().transactionDate(new Date()).book(book.get()).isIssueOperation(true)
                        .transactionStatus(TransactionStatus.FAILED).build();
                transactionRepository5.save(transaction);
                try{
                    throw new Exception("Card is invalid");
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
        }
        else{
            Transaction transaction=Transaction.builder().transactionDate(new Date()).isIssueOperation(true)
                    .transactionStatus(TransactionStatus.FAILED).build();
            transactionRepository5.save(transaction);
            try{
                throw new Exception("Book is either unavailable or not present");
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }

        return transactionId; //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction=null;
        try {
            transaction = transactions.get(transactions.size() - 1);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        Date issuedDate=transaction.getTransactionDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(issuedDate);

        // manipulate date
        cal.add(Calendar.DATE, getMax_allowed_days);
        Date allowedDate = cal.getTime();
        Date currentDate=new Date();
        long time_difference = currentDate.getTime() - allowedDate.getTime();
        // Calculate time difference in days
        long days_difference = (time_difference / (1000*60*60*24)) % 365;
        int fine=(int)days_difference*fine_per_day;

        Card card=cardRepository5.findById(cardId).get();
        Book book=bookRepository5.findById(bookId).get();

        List<Book> books=card.getBooks();
        books.remove(book);
        card.setBooks(books);
        cardRepository5.save(card);

        book.setAvailable(true);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        Transaction returnBookTransaction  = Transaction.builder().transactionDate(new Date()).book(book).card(card)
                .transactionDate(currentDate).fineAmount(fine).transactionStatus(TransactionStatus.SUCCESSFUL).isIssueOperation(false).build();

        List<Transaction> transactionsList=book.getTransactions();
        transactionsList.add(returnBookTransaction);
        book.setTransactions(transactionsList);
        bookRepository5.save(book);
        transactionRepository5.save(returnBookTransaction);
        return returnBookTransaction; //return the transaction after updating all details
    }
}