package me.nuguri.resc.repository;

import me.nuguri.resc.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Clothes, Long> {
}
