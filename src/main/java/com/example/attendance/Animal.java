package com.example.attendance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Animal, hayvan bilgisini temsil eden entity sınıfıdır.
 */
@Entity
@Table(name = "cool_animals")
public class Animal {
    /** Hayvanın benzersiz ID'si */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hayvanın adı */
    private String name;
    /** Hayvanın türü */
    private String species;

    // getters & setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSpecies() {
        return species;
    }
    public void setSpecies(String species) {
        this.species = species;
    }
}
