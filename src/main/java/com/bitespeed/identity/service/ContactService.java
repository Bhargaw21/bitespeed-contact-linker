package com.bitespeed.identity.service;

import com.bitespeed.identity.dto.ContactRequest;
import com.bitespeed.identity.dto.ContactResponse;
import com.bitespeed.identity.entity.Contact;
import com.bitespeed.identity.entity.LinkPrecedence;
import com.bitespeed.identity.repository.ContactRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public ContactResponse identifyContact(ContactRequest request) {
        String email = request.getEmail();
        String phoneNumber = request.getPhoneNumber();

        // Fetch initial matching contacts by email or phone
        List<Contact> matched = contactRepository.findByEmailOrPhoneNumber(email, phoneNumber);
        Set<Contact> allRelatedContacts = new HashSet<>(matched);

        // BFS to find all linked contacts
        Queue<Contact> queue = new LinkedList<>(matched);
        while (!queue.isEmpty()) {
            Contact current = queue.poll();
            List<Contact> linked = contactRepository.findByLinkedId(current.getId());
            for (Contact contact : linked) {
                if (allRelatedContacts.add(contact)) {
                    queue.add(contact);
                }
            }
        }

        // Determine the primary contact (oldest primary)
        Contact primary = allRelatedContacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElse(null);

        if (primary == null) {
            // No primary exists yet, create one
            Contact newPrimary = Contact.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .linkPrecedence(LinkPrecedence.PRIMARY)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Contact saved = contactRepository.save(newPrimary);
            return mapToResponse(saved, List.of(saved));
        }

        //Update all other primaries to secondary
        for (Contact c : allRelatedContacts) {
            if (!c.getId().equals(primary.getId()) && c.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
                c.setLinkPrecedence(LinkPrecedence.SECONDARY);
                c.setLinkedId(primary.getId());
                c.setUpdatedAt(LocalDateTime.now());
                contactRepository.save(c);
            } else if (c.getLinkPrecedence() == LinkPrecedence.SECONDARY && !primary.getId().equals(c.getLinkedId())) {
                c.setLinkedId(primary.getId());
                c.setUpdatedAt(LocalDateTime.now());
                contactRepository.save(c);
            }
        }

        // Check if input is new (email/phone both not found)
        boolean emailExists = allRelatedContacts.stream().anyMatch(c -> email != null && email.equals(c.getEmail()));
        boolean phoneExists = allRelatedContacts.stream().anyMatch(c -> phoneNumber != null && phoneNumber.equals(c.getPhoneNumber()));

        if (!emailExists || !phoneExists) {
            Contact newSecondary = Contact.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .linkPrecedence(LinkPrecedence.SECONDARY)
                    .linkedId(primary.getId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            contactRepository.save(newSecondary);
            allRelatedContacts.add(newSecondary);
        }

        return mapToResponse(primary, allRelatedContacts);
    }

    private ContactResponse mapToResponse(Contact primary, Collection<Contact> contacts) {
        Set<String> emails = contacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> phoneNumbers = contacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> secondaryIds = contacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .toList();

        ContactResponse.Contact responseContact = new ContactResponse.Contact(
                primary.getId(),
                new ArrayList<>(emails),
                new ArrayList<>(phoneNumbers),
                secondaryIds
        );

        return new ContactResponse(responseContact);
    }
}
