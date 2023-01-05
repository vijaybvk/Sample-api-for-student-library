package com.example.library.studentlibrary.controller;

import com.example.library.studentlibrary.models.Student;
import com.example.library.studentlibrary.services.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class StudentController {
    @Autowired
    StudentService studentService;

    @GetMapping("/student/studentById")
    public ResponseEntity getStudentById(@RequestParam("id") int id){
        System.out.println(studentService.getDetailsById(id));
        return new ResponseEntity<>("Student details printed successfully ", HttpStatus.OK);
    }


    @PostMapping("/create-student")
    public ResponseEntity createStudent(@RequestBody Student student){
        studentService.createStudent(student);
        return new ResponseEntity<>("the student is successfully added to the system", HttpStatus.CREATED);
    }


    @PutMapping("/update-student")
    public ResponseEntity updateStudent(@RequestBody Student student){
        studentService.updateStudent(student);
        return new ResponseEntity<>("student is updated", HttpStatus.ACCEPTED);
    }




}