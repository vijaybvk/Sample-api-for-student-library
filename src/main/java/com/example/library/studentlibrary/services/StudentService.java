package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.Student;
import com.example.library.studentlibrary.repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentService {



    @Autowired
    StudentRepository studentRepository;

    public Student getDetailsByEmail(String email){
        Student student = studentRepository.findByEmailId(email);

        return student;
    }

    public Student getDetailsById(int id){
        Student student = studentRepository.findById(id).get();

        return student;
    }

    public void createStudent(Student student){
        studentRepository.save(student);
    }

    public void updateStudent(Student student){
        studentRepository.updateStudentDetails(student);
    }


}