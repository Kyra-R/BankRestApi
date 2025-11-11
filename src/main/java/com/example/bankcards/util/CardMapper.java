package com.example.bankcards.util;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    private static EncryptionUtil encryptionUtil;


    @Autowired
    public CardMapper(EncryptionUtil util) {
        CardMapper.encryptionUtil = util;
    }

    public CardDTO toDTO(Card card) {
        return toDTO(card, false);
    }

   public CardDTO toDTO(Card card, boolean isAdmin){

        if(isAdmin) {

            return new CardDTO(
                    encryptionUtil.decrypt(card.getEncryptedNumber()),
                    card.getOwner().getUsername(),
                    card.getExpirationMonth(),
                    card.getExpirationYear(),
                    card.getStatus(),
                    card.getBalance()
            );

        } else {

           return new CardDTO(
                   CardMaskingUtil.getMaskedNumber(encryptionUtil.decrypt(card.getEncryptedNumber())),
                   card.getOwner().getUsername(),
                   card.getExpirationMonth(),
                   card.getExpirationYear(),
                   card.getStatus(),
                   card.getBalance()
                   );
       }

   }


}
