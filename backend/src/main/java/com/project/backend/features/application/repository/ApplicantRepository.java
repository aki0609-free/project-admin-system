package com.project.backend.features.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.backend.features.application.entity.Applicant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    Optional<Applicant> findByApplicationNo(String applicationNo);

    @EntityGraph(attributePaths = { "applicationMedia" })
    @Query("""
                select a
                from Applicant a
                order by a.id desc
            """)
    List<Applicant> findAllWithApplicationMedia();

    @EntityGraph(attributePaths = { "applicationMedia" })
    @Query("""
                select a
                from Applicant a
                where a.id = :id
            """)
    Optional<Applicant> findDetailById(Long id);

    @Query("""
                select count(a)
                from Applicant a
                where a.applicationMedia.id = :applicationMediaId
                  and a.recruitmentStatus = 'HIRED'
            """)
    long countHiredByApplicationMediaId(Long applicationMediaId);

    @Query("""
                select case when count(a) > 0 then true else false end
                from Applicant a
                where a.applicationMedia.id = :applicationMediaId
                  and a.deletedAt is null
            """)
    boolean existsActiveApplicantByApplicationMediaId(Long applicationMediaId);
}
