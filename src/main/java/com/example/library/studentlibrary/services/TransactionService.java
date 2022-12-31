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
                    Transaction transaction=Transaction.builder().card(card.get()).book(book.get())
                            .transactionStatus(TransactionStatus.SUCCESSFUL)
                            .isIssueOperation(true).transactionDate(new Date()).build();

                    List<Transaction> transactions=book.get().getTransactions();
                    transactions.add(transaction);
                    book.get().setCard(card.get());
                    book.get().setAvailable(false);
                    book.get().setTransactions(transactions);

                    List<Book> books=card.get().getBooks();
                    books.add(book.get());
                    card.get().setBooks(books);

                    transaction.setCard(card.get());

                    bookRepository5.updateBook(book.get());

                    transactionId=transaction.getTransactionId();
                    transactionRepository5.save(transaction);
                }
                else{
                    Transaction transaction=Transaction.builder().transactionDate(new Date()).isIssueOperation(true).book(book.get())
                            .card(card.get()).transactionStatus(TransactionStatus.FAILED).build();
                    transactionRepository5.save(transaction);
                    throw new Exception("Book limit has reached for this card");
                }
            }
            else{
                Transaction transaction=Transaction.builder().transactionDate(new Date()).book(book.get()).isIssueOperation(true)
                        .transactionStatus(TransactionStatus.FAILED).build();
                transactionRepository5.save(transaction);
                throw new Exception("Card is invalid");
            }
        }
        else{
            Transaction transaction=Transaction.builder().transactionDate(new Date()).isIssueOperation(true)
                    .transactionStatus(TransactionStatus.FAILED).build();
            transactionRepository5.save(transaction);
            throw new Exception("Book is either unavailable or not present");
        }

        return transactionId; //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        Book book = bookRepository5.findById(bookId).get();
        Card card = cardRepository5.findById(cardId).get() ;

        Transaction returnTransaction = new Transaction() ;
        returnTransaction.setBook(book);
        returnTransaction.setCard(card);
        returnTransaction.setIssueOperation(false);
        returnTransaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);

        Date issueDate = transaction.getTransactionDate();
        Date returnDate = new Date();

        long d1 = issueDate.getTime();
        long d2 = returnDate.getTime();

        long timeDiff = Math.abs(d2 - d1);

        long daysDiff = TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS);

        int delay = (int)daysDiff - getMax_allowed_days;
        int fine =0;
        if(delay > 0)
        {
            fine = delay*fine_per_day;
        }
        returnTransaction.setFineAmount(fine);
        book.setAvailable(true);

        List<Book> booklist = card.getBooks();
        for(Book b:booklist)
        {
            if(b.getId()== bookId)
                booklist.remove(b) ;
            break ;
        }

        transactionRepository5.save(returnTransaction);


        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        Transaction returnBookTransaction  = returnTransaction;
        return returnBookTransaction; //return the transaction after updating all details
    }
}