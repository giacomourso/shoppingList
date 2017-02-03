package de.yannicklem.shoppinglist.core.article.restapi.controller;

import de.yannicklem.restutils.controller.RestEntityController;
import de.yannicklem.restutils.entity.service.EntityService;
import de.yannicklem.restutils.service.MyResourceProcessor;
import de.yannicklem.restutils.service.RequestHandler;
import de.yannicklem.shoppinglist.core.article.entity.Article;
import de.yannicklem.shoppinglist.core.exception.BadRequestException;
import de.yannicklem.shoppinglist.core.exception.NotFoundException;
import de.yannicklem.shoppinglist.core.user.entity.SLUser;
import de.yannicklem.shoppinglist.core.user.persistence.SLUserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;


@RestController
@RequestMapping(
    value = ArticleEndpoints.ARTICLE_ENDPOINT, produces = {
        MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE
    }
)
@Transactional
@ExposesResourceFor(Article.class)
public class ArticleController extends RestEntityController<Article, Long> {

    private static final Logger LOGGER = getLogger(lookup().lookupClass());

    @Autowired
    public ArticleController(SLUserService slUserService, EntityService<Article, Long> entityService,
                             RequestHandler<Article> requestHandler, MyResourceProcessor<Article> resourceProcessor,
                             EntityLinks entityLinks) {

        super(slUserService, entityService, requestHandler, resourceProcessor, entityLinks);
    }

    @Override
    @RequestMapping(method = RequestMethod.PUT, value = ArticleEndpoints.ARTICLE_SPECIFIC_ENDPOINT)
    public HttpEntity<? extends Article> putEntity(@RequestBody Article entity, @PathVariable("id") Long id,
        Principal principal) {

        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }


    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnused(Principal principal) {

        HttpEntity<? extends Resources<? extends Article>> allEntities = this.getAllEntities(principal);
        Collection<? extends Article> content = allEntities.getBody().getContent();
        SLUser currentUser = principal == null ? null : slUserService.findById(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));

        LOGGER.info("{} removes all unused articles", currentUser == null ? "anonymous" : currentUser.getUsername());

        for (Article article : content) {
            try {
                requestHandler.handleBeforeDelete(article, currentUser);
                entityService.delete(article);
            } catch (BadRequestException e) {
                LOGGER.info("Article '{}' not deleted because: {}", article.getName(), e.getMessage());
            }
        }
    }
}
