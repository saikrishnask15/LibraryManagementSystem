package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryRequest;
import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryResponse;
import com.example.LibraryManagementSystem.dto.mapper.CategoryMapper;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.Category;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryResponse categoryResponse;
    private CategoryRequest categoryRequest;
    private Book book;

    @BeforeEach
    void setUp(){

        //Book entity
        book = new Book();
        book.setId(1);
        book.setTitle("Harry Potter");
        book.setCategories(new ArrayList<>());

        //Category entity
        category = new Category();
        category.setId(10);
        category.setName("Fantasy");
        category.setDescription("Fantasy books");
        category.setBooks(new ArrayList<>());

        //DTO in
        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Fantasy");
        categoryRequest.setDescription("Fantasy books");
        categoryRequest.setBookIds(List.of(1));

        //DTO out
        CategoryResponse.BookDTO bookDTO = new CategoryResponse.BookDTO(1, "Harry Potter");

        categoryResponse = new CategoryResponse();
        categoryResponse.setId(10);
        categoryResponse.setName("Fantasy");
        categoryResponse.setDescription("Fantasy books");
        categoryResponse.setBooks(List.of(bookDTO));
    }


    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("Returns mapped page when valid params are provided")
        void returnsMappedPage() {

            //manually creating a "dummy" pagination result
            Page<Category> categoryPage = new PageImpl<>(List.of(category));
            when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(categoryPage);
            //in categoryPage in that category will be there(actual data) so that we are converting into categoryResponse
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            Page<CategoryResponse> result = categoryService
                    .getAllCategories(null, null, null, 0, 10, "id", "ASC");

            assertEquals(1, result.getTotalElements());
            assertEquals("Fantasy", result.getContent().getFirst().getName());
            assertEquals(1, result.getContent().getFirst().getBooks().size());
            assertEquals("Harry Potter", result.getContent().getFirst().getBooks().getFirst().getTitle());
        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of requested value")
        void capsPageSizeAt50() {

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            // Verify the call and "capture" the argument
            when(categoryRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            categoryService.getAllCategories(null, null, null, 0, 999, "id", "ASC");

            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @ValueSource(strings = {"description", "books", "unknown", ""})
        @DisplayName("Falls back to 'id' for disallowed sortBy values")
        void fallsBackToIdForInvalidSortField(String invalidSortBy) {

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(categoryRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            categoryService.getAllCategories(null, null, null, 0, 999, invalidSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"id", "name"})
        @DisplayName("Accepts all allowed sortBy fields without falling back")
        void acceptsAllowedSortFields(String validSortBy) {

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(categoryRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            categoryService.getAllCategories(null, null, null, 0, 10, validSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor(validSortBy));
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort() {

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(categoryRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            categoryService.getAllCategories(null, null, null, 0, 10, "id", "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("id");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("Returns empty page when no categories match the filter")
        void returnsEmptyPage() {
            when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<CategoryResponse> result = categoryService.getAllCategories(null, "Unknown", null, 0, 10, "id", "ASC");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCategoryById()")
    class GetCategoryById{

        @Test
        @DisplayName("Returns CategoryResponse with nested books when category exists")
        void returnsResponseWhenFound(){

            when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.getCategoryById(10);

            assertEquals(10, result.getId());
            assertEquals("Fantasy", result.getName());
            assertEquals(1, result.getBooks().size());
            assertEquals("Harry Potter", result.getBooks().getFirst().getTitle());

            verify(categoryRepository,times(1)).findById(10);
            verify(categoryMapper, times(1)).toResponse(category);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when category is missing")
        void throwsWhenNotFound(){

            when(categoryRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    ()->categoryService.getCategoryById(1));

            verify(categoryMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("addCategory()")
    class AddCategory{

        @Test
        @DisplayName("Saves category and links books when valid bookIds are provided")
        void savesCategoryWithBooks(){

            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(bookRepository.findAllById(categoryRequest.getBookIds())).thenReturn(List.of(book));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.addCategory(categoryRequest);

            assertEquals(10, result.getId());
            assertEquals("Fantasy", result.getName());
            assertEquals(1, result.getBooks().size());
            assertEquals(1, result.getBooks().getFirst().getId());

            // Category must be saved BEFORE books are linked (service saves first to get the ID)
            verify(categoryRepository).save(category);
            verify(bookRepository).saveAll(List.of(book));

            // Book's categories list must now contain the saved category

            assertTrue((book.getCategories()).contains(category));
        }

        @Test
        @DisplayName("Does not add duplicate — skips book if it already has this category")
        void doesNotAddDuplicateCategoryToBook(){
            // Pre-populate book's categories with the same category
            book.getCategories().add(category);

            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(bookRepository.findAllById(List.of(1))).thenReturn(List.of(book));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.addCategory(categoryRequest);

           assertEquals(1, book.getCategories().size());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when one or more bookIds are invalid")
        void throwsWhenSomeBooksNotFound(){

            categoryRequest.setBookIds(List.of(1,20));

            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(bookRepository.findAllById(List.of(1,20))).thenReturn(List.of(book));

            assertThrows(ResourceNotFoundException.class, ()->categoryService.addCategory(categoryRequest));

            verify(bookRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Saves category without linking books when bookIds is nul")
        void savesCategoryWithNullBookIds(){
            categoryRequest.setBookIds(null);
            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.addCategory(categoryRequest);

           verify(bookRepository, never()).findAllById(any());
           verify(bookRepository, never()).saveAll(any());
           verify(categoryMapper).toResponse(category);
        }

       @Test
       @DisplayName("Saves category without linking books when bookIds is an empty list")
       void savesCategoryWithEmptyBookIds(){
            categoryRequest.setBookIds(List.of());

           when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
           when(categoryRepository.save(category)).thenReturn(category);
           when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

           categoryService.addCategory(categoryRequest);

           verify(bookRepository, never()).findAllById(any());
           verify(bookRepository, never()).saveAll(any());
       }

       @Nested
       @DisplayName("updateCategory()")
       class UpdateCategory{

            @Test
            @DisplayName("Throws ResourceNotFoundException when category to update does not exist")
            void throwsWhenCategoryNotFound(){
                when(categoryRepository.findById(90)).thenReturn(Optional.empty());
                assertThrows(ResourceNotFoundException.class, ()->categoryService.updateCategory(90, categoryRequest));
                verify(bookRepository, never()).save(any());
            }

            @Test
            @DisplayName("Updates name and description when both are provided in request")
            void updatesNameAndDescription(){

                categoryRequest.setName("sci-Fi");
                categoryRequest.setDescription("Science fiction books");
                categoryRequest.setBookIds(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                assertEquals("sci-Fi", category.getName());
                assertEquals("Science fiction books", category.getDescription());

                verify(categoryRepository).save(category);
            }

            @Test
            @DisplayName("Skips name update when name in request is null")
            void skipsNameUpdateWhenNull(){
                categoryRequest.setName(null);
                categoryRequest.setBookIds(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                assertEquals("Fantasy", category.getName());
            }

            @Test
            @DisplayName("Skips description update when description in request is null")
            void skipsDescriptionUpdateWhenNull(){

                categoryRequest.setDescription(null);
                categoryRequest.setBookIds(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                assertEquals("Fantasy books", category.getDescription());
            }

            @Test
            @DisplayName("Unlinks old books and links new books when bookIds list is replaced")
            void replacesBookLinks(){
                // Existing category already has book with ID 1
                book.getCategories().add(category);
                category.setBooks(new ArrayList<>(List.of(book)));

                //New request wants to link book with ID 2 instead

                Book newBook = new Book();
                newBook.setId(2);
                newBook.setTitle("Dune");
                newBook.setCategories(new ArrayList<>());

                categoryRequest.setBookIds(List.of(2));
                categoryRequest.setName(null);
                categoryRequest.setDescription(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.of(category));
                when(bookRepository.findAllById(List.of(2))).thenReturn(List.of(newBook));
                when(bookRepository.saveAll(List.of(book))).thenReturn(List.of(book)); // old books unlinked

                when(bookRepository.saveAll(List.of(newBook))).thenReturn(List.of(newBook)); // new books linked
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);
                // Old book must have this category removed
                assertFalse((book.getCategories()).contains(category));
                // New book must have this category added
                assertTrue((newBook.getCategories()).contains(category));
                verify(categoryRepository).save(category);
            }

            @Test
            @DisplayName("Unlinks all books and sets books to null when bookIds is empty list")
            void unlinksAllBooksWhenBookIdsIsEmpty(){

                book.getCategories().add(category);
                category.setBooks(new ArrayList<>(List.of(book)));

                categoryRequest.setBookIds(List.of());
                categoryRequest.setName(null);
                categoryRequest.setDescription(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(bookRepository.saveAll(List.of(book))).thenReturn(List.of(book));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                assertFalse((book.getCategories()).contains(category));
                assertNull(category.getBooks());
            }

            @Test
            @DisplayName("Skips all book-link logic when bookIds in request is null")
            void skipsBookLinkLogicWhenBookIdsIsNull(){
                categoryRequest.setBookIds(null);
                categoryRequest.setName(null);
                categoryRequest.setDescription(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                verify(bookRepository, never()).saveAll(any());
                verify(bookRepository, never()).findAllById(any());
                verify(categoryRepository, times(1)).save(category);
            }

            @Test
            @DisplayName("Throws ResourceNotFoundException when a new bookId in update is invalid")
            void throwsWhenNewBookNotFound(){
                categoryRequest.setBookIds(List.of(1, 99));
                categoryRequest.setName(null);
                categoryRequest.setDescription(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(bookRepository.findAllById(List.of(1, 99))).thenReturn(List.of(book));

                assertThrows(ResourceNotFoundException.class, ()->categoryService.updateCategory(10, categoryRequest));
                verify(categoryRepository, never()).save(any());
            }

            @Test
            @DisplayName("Skips old-book unlink when category currently has no books")
            void skipsOldBookUnlinkWhenNoBooksAssociated(){
                category.setBooks(new ArrayList<>()); // no existing books

                categoryRequest.setBookIds(List.of(1));
                categoryRequest.setName(null);
                categoryRequest.setDescription(null);

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));
                when(bookRepository.findAllById(List.of(1))).thenReturn(List.of(book));
                when(categoryRepository.save(category)).thenReturn(category);
                when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

                categoryService.updateCategory(10, categoryRequest);

                verify(bookRepository, times(1)).saveAll(List.of(book));

            }

           @Test
           @DisplayName("Does not add duplicate — skips book if it already has this category during update")
           void doesNotAddDuplicateCategoryToBookDuringUpdate() {
               // New book already has this category
               book.getCategories().add(category);
               category.setBooks(new ArrayList<>());

               categoryRequest.setBookIds(List.of(1));
               categoryRequest.setName(null);
               categoryRequest.setDescription(null);

               when(categoryRepository.findById(10)).thenReturn(Optional.of(category));
               when(bookRepository.findAllById(List.of(1))).thenReturn(List.of(book));
               when(categoryRepository.save(category)).thenReturn(category);
               when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

               categoryService.updateCategory(10, categoryRequest);

               // contains() guard — book.categories must still have exactly 1 entry
               assertEquals(1, (book.getCategories()).size());
           }
       }

       @Nested
       @DisplayName("deleteCategory()")
       class DeleteCategory {

            @Test
            @DisplayName("Deletes category and removes it from all associated books")
            void deletesCategoryAndUnlinksBooks(){
                book.getCategories().add(category);
                category.setBooks(new ArrayList<>(List.of(book)));
                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));

                categoryService.deleteCategory(10);

                assertFalse(book.getCategories().contains(category));
                verify(bookRepository).saveAll(category.getBooks());
                verify(categoryRepository).delete(category);
            }

            @Test
            @DisplayName("Deletes category directly when it has no associated books")
            void deletesCategoryWithNoBooks(){
                categoryRequest.setBookIds(new ArrayList<>());

                when(categoryRepository.findById(10)).thenReturn(Optional.ofNullable(category));

                categoryService.deleteCategory(10);

                // No book operations needed
                verify(bookRepository, never()).saveAll(any());
                verify(categoryRepository).delete(category);
            }

           @Test
           @DisplayName("Throws ResourceNotFoundException when category to delete does not exist")
           void throwsWhenCategoryNotFound() {
               when(categoryRepository.findById(99)).thenReturn(Optional.empty());

               assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(99));

               verify(bookRepository, never()).saveAll(any());
               verify(categoryRepository, never()).delete((Category) any());
           }

           @Test
           @DisplayName("Removes category from multiple books before deleting")
           void removesCategoryFromMultipleBooksBeforeDeleting() {
               Book book2 = new Book();
               book2.setId(2);
               book2.setTitle("Dune");
               book2.setCategories(new ArrayList<>(List.of(category)));

               book.getCategories().add(category);
               category.setBooks(new ArrayList<>(List.of(book, book2)));

               when(categoryRepository.findById(10)).thenReturn(Optional.of(category));

               categoryService.deleteCategory(10);

               assertFalse((book.getCategories()).contains(category));
               assertFalse((book2.getCategories()).contains(category));
               verify(categoryRepository).delete(category);
           }
       }
    }
}