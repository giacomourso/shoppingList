package de.yannicklem.shoppinglist.core.persistence;

import de.yannicklem.shoppinglist.core.article.entity.Article;
import de.yannicklem.shoppinglist.core.item.entity.Item;
import de.yannicklem.shoppinglist.core.list.entity.ShoppingList;
import de.yannicklem.shoppinglist.core.user.entity.SLUser;
import de.yannicklem.shoppinglist.core.user.security.service.CurrentUserService;
import de.yannicklem.shoppinglist.exception.AlreadyExistsException;
import de.yannicklem.shoppinglist.exception.NotFoundException;
import de.yannicklem.shoppinglist.restutils.service.EntityService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired ))
public class ItemService implements EntityService<Item, Long> {

    private final ItemValidationService itemValidationService;
    private final ShoppingListService shoppingListService;
    private final ItemRepository itemRepository;
    private final CurrentUserService currentUserService;

    public void handleBeforeCreate(Item item) {

        if (item != null && item.getArticle() != null) {
            item.getArticle().getOwners().addAll(item.getOwners());
        }

        if (item != null && exists(item.getEntityId())) {
            throw new AlreadyExistsException("Item already exists");
        }

        itemValidationService.validate(item);
    }


    public void handleBeforeUpdate(Item item) {

        if (item != null && item.getArticle() != null) {
            item.getArticle().getOwners().addAll(item.getOwners());
        }

        if (item == null || !exists(item.getEntityId())) {
            throw new NotFoundException("Item not found");
        }

        itemValidationService.validate(item);
    }


    @Override
    public Item findById(Long id) {

        if (id == null) {
            return null;
        }

        return itemRepository.findOne(id);
    }


    @Override
    public List<Item> findAll(SLUser currentUser) {

        if (currentUser == null || currentUser.isAdmin()) {
            return itemRepository.findAll();
        } else {
            return itemRepository.findItemsOwnedBy(currentUser);
        }
    }


    @Override
    public boolean exists(Long id) {

        if (id == null) {
            return false;
        }

        return itemRepository.exists(id);
    }


    @Override
    public Item create(Item entity) {

        handleBeforeCreate(entity);

        return itemRepository.save(entity);
    }


    @Override
    public Item update(Item entity) {

        handleBeforeUpdate(entity);

        return itemRepository.save(entity);
    }


    @Override
    public void delete(Item entity) {

        if (entity == null) {
            throw new NotFoundException("Item not found");
        }

        SLUser currentUser = currentUserService.getCurrentUser();
        entity.getOwners().remove(currentUser);

        handleBeforeDelete(entity);

        itemRepository.delete(entity);
    }


    private void handleBeforeDelete(Item entity) {

        if (entity == null || !exists(entity.getEntityId())) {
            throw new NotFoundException("Item not found");
        }

        List<ShoppingList> shoppingListsContainingItem = shoppingListService.findShoppingListsContainingItem(entity);

        for (ShoppingList shoppingList : shoppingListsContainingItem) {
            shoppingList.getItems().remove(entity);
            shoppingListService.update(shoppingList);
        }
    }


    @Override
    public void deleteAll() {

        List<Item> all = findAll(currentUserService.getCurrentUser());

        for (Item item : all) {
            delete(item);
        }
    }


    public List<Item> findItemsOwnedBy(SLUser slUser) {

        if (slUser == null) {
            return new ArrayList<>();
        }

        return itemRepository.findItemsOwnedBy(slUser);
    }


    public List<Item> findItemsByArticle(Article article) {

        if (article == null) {
            return new ArrayList<>();
        }

        return itemRepository.findByArticle(article);
    }


    public List<Item> findUnusedItems(Date date) {

        return itemRepository.findUnusedItems(date);
    }
}
