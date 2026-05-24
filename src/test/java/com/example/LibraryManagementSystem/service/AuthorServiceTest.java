package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.dto.mapper.AuthorMapper;
import com.example.LibraryManagementSystem.exception.ConflictException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Author;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.repository.AuthorRepository;
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

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito without loading Spring context
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository; // create a fake version of the repo

    @Mock
    private AuthorMapper authorMapper; // create a fake version of the mapper

    @InjectMocks
    private AuthorService authorService; // injects the above mocks into the real service

    private Author author;
    private AuthorRequest authorRequest;
    private AuthorResponse authorResponse;

    @BeforeEach
    void setup(){
        author = new Author();
        author.setId(1);
        author.setName("J.K. Rowling");
        author.setEmail("jk@example.com");
        author.setBio("American writer");
        author.setBooks(new ArrayList<>());

        authorRequest = new AuthorRequest();
        authorRequest.setName("J.K. Rowling");
        authorRequest.setEmail("jk@example.com");
        authorRequest.setBio("American writer");

        authorResponse = new AuthorResponse();
        authorResponse.setId(1);
        authorResponse.setName("J.K. Rowling");
        authorResponse.setEmail("jk@example.com");
        authorResponse.setBio("American writer");
    }

    @Nested
    @DisplayName("getAllAuthors()")
    class GetAllAuthors{

        @Test
        @DisplayName("Returns a mapped page when valid params are provided")
        void returnsPageOfAuthorResponses(){
            Page<Author> authorPage = new PageImpl<>(List.of(author));
            when(authorRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(authorPage);
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            Page<AuthorResponse> result = authorService.getAllAuthors(
                    null, null, null, 0, 10, "id", "ASC"
            );
            assertEquals(1, result.getTotalElements());
            assertEquals("J.K. Rowling", result.getContent().getFirst().getName());
        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of the requested size")
        void capsPageSizeAt50(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(authorRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            authorService.getAllAuthors(
                    null, null, null, 0, 999, "id", "ASC");
            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @ValueSource(strings = {"bio", "country", "unknown", ""})
        @DisplayName("Falls back to sort by 'id' for disallowed sortBy values")
        void fallsBackToSortById(String invalidSortBy){

            ArgumentCaptor<Pageable> captor =ArgumentCaptor.forClass(Pageable.class);
            when(authorRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            authorService.getAllAuthors(
                    null, null, null, 0, 10, invalidSortBy, "ASC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("id");
            assertNotNull(order);
        }

        @ParameterizedTest
        @ValueSource(strings = {"id", "name", "email"})
        @DisplayName("Accepts all allowed sortBy fields without falling back")
        void acceptsAllowedSortFields(String validSortBy) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(authorRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            authorService.getAllAuthors(null, null, null, 0, 10, validSortBy, "ASC");

            Sort.Order order = captor.getValue().getSort().getOrderFor(validSortBy);
            assertNotNull(order);
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(authorRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            authorService.getAllAuthors(null, null, null, 0, 10, "name", "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("name");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("Returns empty page when no authors match the filter")
        void returnsEmptyPageWhenNoMatch() {
            when(authorRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<AuthorResponse> result = authorService.getAllAuthors(
                    "Nobody", null, null, 0, 10, "id", "ASC");

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }


    @Nested  // Groups related tests for readability
    @DisplayName("getAuthorById()")
    class GetAuthorById{

        @Test
        @DisplayName("Returns AuthorResponse when author exists")
        void returnsResponseWhenFound(){
            Integer authorId = 1;
            // Tell the mocks how to behave
            when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            // ACT (The "Actual" result)
            AuthorResponse result = authorService.getAuthorById(authorId);

            //assert
            assertNotNull(result);
            // Comparing against the object we created in setup()
            assertEquals( author.getId(), result.getId());
            assertEquals(author.getEmail(), result.getEmail());

            // Ensure findById was called exactly once
            verify(authorRepository, times(1)).findById(authorId);
            // Ensure authorMapper was called exactly once
            verify(authorMapper, times(1)).toResponse(author);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when author does not exist")
        void ThrowsWhenNotFound() {
            Integer nonExistedId = 99;

            // Tell the mock to return an empty Optional
            when(authorRepository.findById(nonExistedId)).thenReturn(Optional.empty());

            //ACT & ASSERT
            assertThrows(ResourceNotFoundException.class, () -> authorService.getAuthorById(nonExistedId));

            //Ensure the mapper was NEVER called because the code crashed before reaching it
            verify(authorMapper, never()).toResponse(any());

            verify(authorRepository, times(1)).findById(nonExistedId);
        }
    }

    @Nested
    @DisplayName("addAuthor()")
    class AddAuthor{

        @Test
        @DisplayName("Saves and returns AuthorResponse when no email conflict")
        void returnsAuthorSuccessfully(){

           when(authorRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(authorMapper.toEntity(authorRequest)).thenReturn(author);
            when(authorRepository.save(author)).thenReturn(author);
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            AuthorResponse result = authorService.addAuthor(authorRequest);

            assertNotNull(result);
            assertEquals(author.getId(), authorResponse.getId());
            assertEquals(author.getName(), authorResponse.getName());
            verify(authorRepository, times(1)).save(author);
        }

        @Test
        @DisplayName("Throws ResourceAlreadyExistsException when email is already registered")
        void throwsWhenEmailAlreadyExists(){

            when(authorRepository.findByEmail("jk@example.com")).thenReturn(Optional.of(author));
            assertThrows(ResourceAlreadyExistsException.class, ()->authorService.addAuthor(authorRequest));

            verify(authorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Skips email uniqueness check and saves when email is null")
        void savesWhenEmailIsNull(){

            authorRequest.setEmail(null);
            when(authorMapper.toEntity(authorRequest)).thenReturn(author);
            when(authorRepository.save(author)).thenReturn(author);
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            AuthorResponse result = authorService.addAuthor(authorRequest);

            assertNotNull(result);
            assertEquals(author.getId(), authorResponse.getId());
            assertEquals(author.getName(), authorResponse.getName());

            verify(authorRepository, times(1)).save(author);
        }
    }

    @Nested
    @DisplayName("updateAuthor()")
    class UpdateAuthor {

        @Test
        @DisplayName("Updates successfully when email is unchanged")
        void updatesWithSameEmail() {

            when(authorRepository.findById(1)).thenReturn(Optional.of(author));
            when(authorRepository.save(author)).thenReturn(author);
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            AuthorResponse result = authorService.updateAuthor(1, authorRequest);

            assertNotNull(result);
            verify(authorMapper).updateEntityFromRequest(author, authorRequest);
            verify(authorRepository).save(author);
            verify(authorRepository, never()).findByEmail(any());
        }
    }

    @Test
    @DisplayName("Updates successfully when new email is unique")
    void updatesWithNewUniqueEmail() {
        authorRequest.setEmail("new@example.com"); // different from existing "jk@example.com"

        when(authorRepository.findById(1)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        AuthorResponse result = authorService.updateAuthor(1, authorRequest);

        assertNotNull(result);
        verify(authorRepository).findByEmail("new@example.com");
        verify(authorRepository).save(author);
    }

    @Test
    @DisplayName("Throws ResourceAlreadyExistsException when new email belongs to another author")
    void throwsWhenEmailTaken(){

        Author anotherAuthor = new Author();
        anotherAuthor.setId(2);
        anotherAuthor.setEmail("taken@example.com");

        authorRequest.setEmail("taken@example.com");
        when(authorRepository.findById(1)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(anotherAuthor));

        assertThrows(ResourceAlreadyExistsException.class, ()->{
           authorService.updateAuthor(1, authorRequest);
        });

        verify(authorRepository, never()).save(author);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when new id not available")
    void throwsWhenAuthorIdNotFound(){

        when(authorRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, ()->{
            authorService.updateAuthor(99, authorRequest);
        });

        verify(authorRepository, never()).save(any());
        verify(authorMapper, never()).updateEntityFromRequest(author, authorRequest);
    }

    @Test
    @DisplayName("Skips email uniqueness check when request email is null")
    void skipsEmailCheckWhenNull(){
            authorRequest.setEmail(null);

            when(authorRepository.findById(1)).thenReturn(Optional.of(author));
            when(authorRepository.save(author)).thenReturn(author);
            when(authorMapper.toResponse(author)).thenReturn(authorResponse);

            authorService.updateAuthor(1, authorRequest);

           verify(authorRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Updates bio to null when bio is cleared (bio is optional)")
    void updatesBioToNull(){
        authorRequest.setBio(null);

        when(authorRepository.findById(1)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        authorService.updateAuthor(1, authorRequest);

        verify(authorMapper).updateEntityFromRequest(author, authorRequest);
        verify(authorRepository, times(1)).save(author);
    }

    @Nested
    @DisplayName("deleteAuthor()")
    class DeleteAuthor{

        @Test
        @DisplayName("Deletes author when book list is empty")
        void deleteWhenNoBooksAssociated(){
            author.setBooks(new ArrayList<>());

            when(authorRepository.findById(1)).thenReturn(Optional.of(author));

            authorService.deleteAuthor(1);

            verify(authorRepository).delete(author);
        }
    }

    @Test
    @DisplayName("Throws ConflictException when author has one associated book")
    void throwsWhenAuthorHasSingleBook(){
        author.setBooks(new ArrayList<>(List.of(new Book())));

        when(authorRepository.findById(1)).thenReturn(Optional.of(author));

        assertThrows(ConflictException.class, ()->{
           authorService.deleteAuthor(1);
        });
        verify(authorRepository, never()).delete(any(Author.class));
    }

    @Test
    @DisplayName("Throws ConflictException when author has multiple associated books")
    void throwsWhenAuthorHasMultipleBooks() {
        author.setBooks(new ArrayList<>(List.of(new Book(), new Book(), new Book())));
        when(authorRepository.findById(1)).thenReturn(Optional.of(author));

        assertThrows(ConflictException.class, () -> {
            authorService.deleteAuthor(1);
        });
        verify(authorRepository, never()).delete(any(Author.class));
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when author does not exist")
    void throwsWhenAuthorNotFound() {
        when(authorRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            authorService.deleteAuthor(99);
        });

        verify(authorRepository, never()).delete(any(Author.class));
    }

}