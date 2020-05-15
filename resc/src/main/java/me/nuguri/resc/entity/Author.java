package me.nuguri.resc.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = false)
public class Author extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private LocalDate birth;

    private LocalDate death;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<Book> books = new ArrayList<>();

}