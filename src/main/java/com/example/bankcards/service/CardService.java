package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.IncorrectOwnerException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.WrongCardStatusException;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.EncryptionUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CardService {

    private final CardsRepository cardsRepository;

    private final UsersRepository usersRepository;

    private final CardMapper cardMapper;

    private final EncryptionUtil encryptionUtil;

    @Autowired
    public CardService(CardsRepository cardsRepository, UsersRepository usersRepository, CardMapper cardMapper, EncryptionUtil encryptionUtil){
        this.cardsRepository = cardsRepository;
        this.usersRepository = usersRepository;
        this.cardMapper = cardMapper;
        this.encryptionUtil = encryptionUtil;
    }

    public Page<CardDTO> getAllCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Card> cardsPage = cardsRepository.findAll(pageable);

        return cardsPage.map(card -> cardMapper.toDTO(card, true));
    }


    public Page<CardDTO> getUserCards(String username, int page, int size) {
        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Card> cardsPage = cardsRepository.findByOwner(user, pageable);

        return cardsPage.map(cardMapper::toDTO);
    }


    public BigDecimal checkCardBalance(String username, String cardNumber) {
        String encryptedNumber = encryptionUtil.encrypt(cardNumber);

        Card card = cardsRepository.findByEncryptedNumber(encryptedNumber)
                .orElseThrow(() -> new CardNotFoundException());

        if (!card.getOwner().getUsername().equals(username)) {
            throw new IncorrectOwnerException(username);
        }


        return card.getBalance();
    }


    @Transactional
    public void requestBlockCard(String username, String number) {
        Card card = cardsRepository.findByEncryptedNumber(encryptionUtil.encrypt(number))
                .orElseThrow(() -> new CardNotFoundException());

        if (!card.getOwner().getUsername().equals(username)) {
            throw new IncorrectOwnerException(username);
        }

        if (card.getStatus() == CardStatus.BLOCK_REQUESTED) {
            throw new WrongCardStatusException("Запрос на блокировку уже отправлен");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new WrongCardStatusException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCK_REQUESTED);
        cardsRepository.save(card);
    }

    @Transactional
    public void changeCardStatus(String number, String status) {
        Card card = cardsRepository.findByEncryptedNumber(encryptionUtil.encrypt(number))
                .orElseThrow(() -> new CardNotFoundException());


        try {
            CardStatus newStatus = CardStatus.valueOf(status.toUpperCase());
            card.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new WrongCardStatusException(status);
        }

        cardsRepository.save(card);

    }


    @Transactional
    public void transfer(String username, String fromCardNumber, String toCardNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть больше 0");
        }

        String encryptedFrom = encryptionUtil.encrypt(fromCardNumber);
        String encryptedTo = encryptionUtil.encrypt(toCardNumber);

        Card fromCard = cardsRepository.findByEncryptedNumber(encryptedFrom)
                .orElseThrow(() -> new CardNotFoundException("Исходная карта"));
        Card toCard = cardsRepository.findByEncryptedNumber(encryptedTo)
                .orElseThrow(() -> new CardNotFoundException("Целевая карта"));

        if (!fromCard.getOwner().getUsername().equals(username)) {
            throw new IncorrectOwnerException(username);
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE || toCard.getStatus() != CardStatus.ACTIVE) {
            throw new WrongCardStatusException("Исходная:" + fromCard.getStatus() + " Целевая:" + toCard.getStatus());
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Недостаточно средств на карте");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardsRepository.save(fromCard);
        cardsRepository.save(toCard);
    }


    public Page<CardDTO> checkBlockingRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Card> cardsPage = cardsRepository.findCardsByStatus(CardStatus.BLOCK_REQUESTED, pageable);

        return cardsPage.map(card -> cardMapper.toDTO(card, true));
    }

    @Transactional
    public CardDTO createCard(CardDTO cardDTO) {
        User owner = usersRepository.findByUsername(cardDTO.getOwnerUsername())
                .orElseThrow(() -> new UserNotFoundException(cardDTO.getOwnerUsername()));

        if (cardDTO.getMaskedNumber() == null || cardDTO.getMaskedNumber().isBlank()) {
            throw new IllegalArgumentException("Номер карты не может быть пустым");
        }

        String encryptedNumber = encryptionUtil.encrypt(cardDTO.getMaskedNumber());

        Card card = new Card();
        card.setEncryptedNumber(encryptedNumber);
        card.setExpirationMonth(cardDTO.getExpirationMonth());
        card.setExpirationYear(cardDTO.getExpirationYear());
        card.setOwner(owner);
        card.setBalance(cardDTO.getBalance() != null ? cardDTO.getBalance() : BigDecimal.ZERO);
        card.setStatus(cardDTO.getStatus() != null ? cardDTO.getStatus() : CardStatus.ACTIVE);

        Card saved = cardsRepository.save(card);

        CardDTO result = cardMapper.toDTO(saved);
        return result;
    }


    @Transactional
    public CardDTO updateCard(String number, CardDTO cardDTO) {
        Card card = cardsRepository.findByEncryptedNumber(encryptionUtil.encrypt(number))
                .orElseThrow(() -> new CardNotFoundException());

        if (cardDTO.getMaskedNumber() != null && !cardDTO.getMaskedNumber().isBlank()) {
            String encrypted = encryptionUtil.encrypt(cardDTO.getMaskedNumber());
            card.setEncryptedNumber(encrypted);
        }

        if (cardDTO.getExpirationMonth() > 0) {
            card.setExpirationMonth(cardDTO.getExpirationMonth());
        }

        if (cardDTO.getExpirationYear() > 0) {
            card.setExpirationYear(cardDTO.getExpirationYear());
        }

        if (cardDTO.getStatus() != null) {
            try {
                card.setStatus(cardDTO.getStatus());
            } catch (IllegalArgumentException e) {
                throw new WrongCardStatusException(cardDTO.getStatus().name());
            }
        }

        if (cardDTO.getBalance() != null) {
            card.setBalance(cardDTO.getBalance());
        }

        if (cardDTO.getOwnerUsername() != null) {
            User newOwner = usersRepository.findByUsername(cardDTO.getOwnerUsername())
                    .orElseThrow(() -> new UserNotFoundException(cardDTO.getOwnerUsername()));
            card.setOwner(newOwner);
        }

        Card updated = cardsRepository.save(card);
        CardDTO result = cardMapper.toDTO(updated);
        return result;
    }


    public void deleteCard(String number){
        Card card = cardsRepository.findByEncryptedNumber(encryptionUtil.encrypt(number))
                .orElseThrow(() -> new CardNotFoundException());

        cardsRepository.delete(card);
    }

}
