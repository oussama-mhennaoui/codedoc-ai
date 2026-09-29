package com.codedoc.doc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocSectionRepository extends JpaRepository<DocSection, Long> {
}
