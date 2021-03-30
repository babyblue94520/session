package pers.clare.demo.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pers.clare.demo.data.entity.Session;

public interface SessionRepository extends JpaRepository<Session,String> {
}
