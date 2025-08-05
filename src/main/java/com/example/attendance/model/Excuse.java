package com.example.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "excuse")
public class Excuse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Users tablosundaki @Id
    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="excuse_date", nullable=false)
    private LocalDate excuseDate;

    @Column(name="excuse_type", nullable=false)
    private int excuseType;
    /* Mazeret Türü
     * 0->İzinli
     * 1->(Yıllık izin olmadan) Mazeretli
    */

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @Column(name="is_approved", nullable=false)
    private Boolean isApproved = false;


    public Excuse(Long userId, LocalDate excuseDate, int excuseType, String description) {
        this.userId = userId;
        this.excuseDate = excuseDate;
        this.excuseType = excuseType;
        this.description = description;
    }

}
