package com.example.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "week_start", nullable = false)
    private String weekStart;

    @Column(name = "monday", nullable = false)
    private int monday;

    @Column(name = "tuesday", nullable = false)
    private int tuesday;

    @Column(name = "wednesday", nullable = false)
    private int wednesday;

    @Column(name = "thursday", nullable = false)
    private int thursday;

    @Column(name = "friday", nullable = false)
    private int friday;

  /* Gün bilgisi 
   * 0-> Veri girişi yok
   * 1-> Ofiste
   * 2-> Uzaktan
   * 3-> İzinli
   * 4-> (Yıllık İzin olmadan) Mazeretli
   * 5-> Resmi Tatil
   */

 
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    public Attendance(String userId, String weekStart) {
      this.userId = userId;
      this.weekStart = weekStart;
      this.monday = 0;
      this.tuesday = 0;
      this.wednesday = 0;
      this.thursday = 0;
      this.friday = 0;
      this.isApproved = false;
    }

    public List<Integer> getDates(){
      return List.of(monday, tuesday, wednesday, thursday, friday);
    }

}
