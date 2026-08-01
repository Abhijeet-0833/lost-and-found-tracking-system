package com.lostfound.service;


import com.lostfound.model.Item;
import com.lostfound.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<Item> findPotentialMatches(Item newItem) {
        // If user reports a LOST item, search for FOUND items with similar titles
        String searchType = newItem.getItemType().equals("LOST") ? "FOUND" : "LOST";
        
        // This logic finds items of the opposite type with the same title
        return itemRepository.findByItemTypeAndTitleContainingIgnoreCase(searchType, newItem.getTitle());
    }
}