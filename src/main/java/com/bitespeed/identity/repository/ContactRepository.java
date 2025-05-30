package com.bitespeed.identity.repository;

import com.bitespeed.identity.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Find contacts by email or phoneNumber
    List<Contact> findByEmailOrPhoneNumber(String email, String phoneNumber);

    // Optional: Find all contacts linked to a particular primary contact
    List<Contact> findByLinkedId(Long linkedId);

    // Optional: Find contacts by linkPrecedence
    List<Contact> findByLinkPrecedence(String linkPrecedence);
}

