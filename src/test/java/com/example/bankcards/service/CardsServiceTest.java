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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardsServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock
    private CardsRepository cardsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllCards_ReturnsPageOfCardDTOs() {
        Card card = new Card();
        Page<Card> page = new PageImpl<>(List.of(card));
        when(cardsRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cardMapper.toDTO(card, true)).thenReturn(new CardDTO("1111222233334444", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.valueOf(100)));

        Page<CardDTO> result = cardService.getAllCards(0, 10);

        assertEquals(1, result.getContent().size());
        verify(cardsRepository).findAll(any(Pageable.class));
    }

    @Test
    void getUserCards_ReturnsCards() {
        User user = new User();
        user.setUsername("user1");
        Card card = new Card();
        Page<Card> page = new PageImpl<>(List.of(card));

        when(usersRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(cardsRepository.findByOwner(user, PageRequest.of(0, 10, Sort.by("id").descending()))).thenReturn(page);
        when(cardMapper.toDTO(card)).thenReturn(new CardDTO("1111222233334444", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.valueOf(100)));

        Page<CardDTO> result = cardService.getUserCards("user1", 0, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUserCards_ThrowsWhenUserNotFound() {
        when(usersRepository.findByUsername("user1")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> cardService.getUserCards("user1", 0, 10));
    }


    @Test
    void checkCardBalance_ThrowsIfWrongOwner() {
        User owner = new User();
        owner.setUsername("owner1");
        Card card = new Card();
        card.setOwner(owner);
        card.setBalance(BigDecimal.TEN);

        when(encryptionUtil.encrypt("1234")).thenReturn("encrypted1234");
        when(cardsRepository.findByEncryptedNumber("encrypted1234")).thenReturn(Optional.of(card));

        assertThrows(IncorrectOwnerException.class, () -> cardService.checkCardBalance("someoneElse", "1234"));
    }

    @Test
    void checkCardBalance_ReturnsBalance() {
        User owner = new User();
        owner.setUsername("owner1");
        Card card = new Card();
        card.setOwner(owner);
        card.setBalance(BigDecimal.TEN);

        when(encryptionUtil.encrypt("1234")).thenReturn("encrypted1234");
        when(cardsRepository.findByEncryptedNumber("encrypted1234")).thenReturn(Optional.of(card));

        BigDecimal balance = cardService.checkCardBalance("owner1", "1234");
        assertEquals(BigDecimal.TEN, balance);
    }

    @Test
    void requestBlockCard_Success() {
        User user = new User();
        user.setUsername("user1");
        Card card = new Card();
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);

        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));

        cardService.requestBlockCard("user1", "1111");

        assertEquals(CardStatus.BLOCK_REQUESTED, card.getStatus());
        verify(cardsRepository).save(card);
    }

    @Test
    void requestBlockCard_ThrowsWhenAlreadyBlocked() {
        User user = new User();
        user.setUsername("user1");
        Card card = new Card();
        card.setOwner(user);
        card.setStatus(CardStatus.BLOCKED);

        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));

        assertThrows(Exception.class, () -> cardService.requestBlockCard("user1", "1111"));
    }

    @Test
    void checkBlockingRequests_returnsPageOfCardDTOs() {
        User user = new User();
        user.setUsername("user1");
        Card card = new Card();
        card.setOwner(user);
        card.setStatus(CardStatus.BLOCKED);

        Page<Card> cardsPage = new PageImpl<>(List.of(card));
        when(cardsRepository.findCardsByStatus(
                any(CardStatus.class),
                any(PageRequest.class)
        )).thenReturn(cardsPage);


        CardDTO dto = new CardDTO("1111222233334444", "user1", 12, 2025, CardStatus.BLOCK_REQUESTED, BigDecimal.valueOf(100));
        when(cardMapper.toDTO(card, true)).thenReturn(dto);

        Page<CardDTO> result = cardService.checkBlockingRequests(0, 10);


        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }



    @Test
    void changeCardStatus_Success() {
        Card card = new Card();
        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));

        cardService.changeCardStatus("1111", "ACTIVE");

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardsRepository).save(card);
    }

    @Test
    void changeCardStatus_ThrowsOnInvalidStatus() {
        Card card = new Card();
        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));

        assertThrows(WrongCardStatusException.class, () -> cardService.changeCardStatus("1111", "UNKNOWN"));
    }


    @Test
    void transfer_Success() {
        User user = new User();
        user.setUsername("user1");

        Card from = new Card();
        from.setOwner(user);
        from.setStatus(CardStatus.ACTIVE);
        from.setBalance(BigDecimal.valueOf(100));

        Card to = new Card();
        to.setOwner(user);
        to.setStatus(CardStatus.ACTIVE);
        to.setBalance(BigDecimal.valueOf(50));

        when(encryptionUtil.encrypt("from")).thenReturn("encryptedFrom");
        when(encryptionUtil.encrypt("to")).thenReturn("encryptedTo");
        when(cardsRepository.findByEncryptedNumber("encryptedFrom")).thenReturn(Optional.of(from));
        when(cardsRepository.findByEncryptedNumber("encryptedTo")).thenReturn(Optional.of(to));

        cardService.transfer("user1", "from", "to", BigDecimal.valueOf(40));

        assertEquals(BigDecimal.valueOf(60), from.getBalance());
        assertEquals(BigDecimal.valueOf(90), to.getBalance());
        verify(cardsRepository, times(2)).save(any());
    }



    @Test
    void transfer_ThrowsIfInsufficientBalance() {
        User user = new User();
        user.setUsername("user1");

        Card from = new Card();
        from.setOwner(user);
        from.setStatus(CardStatus.ACTIVE);
        from.setBalance(BigDecimal.valueOf(10));

        Card to = new Card();
        to.setOwner(user);
        to.setStatus(CardStatus.ACTIVE);
        to.setBalance(BigDecimal.valueOf(50));

        when(encryptionUtil.encrypt("from")).thenReturn("encryptedFrom");
        when(encryptionUtil.encrypt("to")).thenReturn("encryptedTo");
        when(cardsRepository.findByEncryptedNumber("encryptedFrom")).thenReturn(Optional.of(from));
        when(cardsRepository.findByEncryptedNumber("encryptedTo")).thenReturn(Optional.of(to));

        assertThrows(RuntimeException.class, () -> cardService.transfer("user1", "from", "to", BigDecimal.valueOf(40)));
    }


    @Test
    void createCard_Success() {
        User owner = new User();
        owner.setUsername("user1");
        Card card = new Card();

        when(usersRepository.findByUsername("user1")).thenReturn(Optional.of(owner));
        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toDTO(card)).thenReturn(new CardDTO("1111", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.ZERO));

        CardDTO result = cardService.createCard(new CardDTO("1111", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.ZERO));

        assertNotNull(result);
        verify(cardsRepository).save(any(Card.class));
    }

    @Test
    void createCard_ThrowsIfOwnerNotFound() {
        when(usersRepository.findByUsername("user1")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () ->
                cardService.createCard(new CardDTO("1111", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.ZERO))
        );
    }


    @Test
    void updateCard_Success() {
        User user = new User();
        user.setUsername("user1");
        Card card = new Card();
        card.setOwner(user);

        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));
        when(usersRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(cardsRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toDTO(card)).thenReturn(new CardDTO("1111", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.ZERO));

        CardDTO result = cardService.updateCard("1111", new CardDTO("1111", "user1", 12, 2025, CardStatus.ACTIVE, BigDecimal.ZERO));

        assertNotNull(result);
        verify(cardsRepository).save(card);
    }


    @Test
    void deleteCard_Success() {
        Card card = new Card();
        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.of(card));

        cardService.deleteCard("1111");

        verify(cardsRepository).delete(card);
    }

    @Test
    void deleteCard_ThrowsIfNotFound() {
        when(encryptionUtil.encrypt("1111")).thenReturn("encrypted1111");
        when(cardsRepository.findByEncryptedNumber("encrypted1111")).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.deleteCard("1111"));
    }
}
