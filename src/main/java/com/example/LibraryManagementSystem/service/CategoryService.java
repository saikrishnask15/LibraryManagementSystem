package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryRequest;
import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryResponse;
import com.example.LibraryManagementSystem.dto.mapper.CategoryMapper;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.Category;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.CategoryRepository;
import com.example.LibraryManagementSystem.specification.CategorySpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final BookRepository bookRepository;

    private final CategoryMapper categoryMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS  = Set.of("id", "name");

    public Page<CategoryResponse> getAllCategories(
          Integer id,  String name, List<Integer> bookIds, Integer pageNo, Integer pageSize, String sortBy, String sortDir) {

        pageSize = Math.min(pageSize, 50);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "id";
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Specification<Category> spec = CategorySpecification.filterCategories(id, name, bookIds);

        Page<Category> categoryPage = categoryRepository.findAll(spec, pageable);

        return categoryPage.map(categoryMapper::toResponse);
    }

    public CategoryResponse getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","id",categoryId));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse addCategory(CategoryRequest request) {

        // Creating category entity
        //using mapper to convert DTO to entity
        Category category = categoryMapper.toEntity(request);

        // IMPORTANT: Save category FIRST to get the ID
        Category savedCategory = categoryRepository.save(category);

        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            List<Book> newBooks = bookRepository.findAllById(request.getBookIds());

            // Validate all books exist
            if (newBooks.size() != request.getBookIds().size()) {
                throw new ResourceNotFoundException("One or more books not found");
            }

            // Adding category to books (owning side)
            for (Book book : newBooks) {
                if (!book.getCategories().contains(savedCategory)) {
                    book.getCategories().add(savedCategory);
                }
            }
            // saving the books to update in join table
            bookRepository.saveAll(newBooks);

            savedCategory.setBooks(newBooks);
        }
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Integer categoryId, CategoryRequest request) {

        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","id",categoryId));

        if(request.getName() != null){
            existingCategory.setName(request.getName());
        }
        if(request.getDescription() != null){
            existingCategory.setDescription(request.getDescription());
        }
        //&& !request.getBookIds().isEmpty()
        if(request.getBookIds() != null ){

            // Removing old links (Owning side)
            if (existingCategory.getBooks() != null && !existingCategory.getBooks().isEmpty()) {
                for (Book oldBook : existingCategory.getBooks()) {
                    oldBook.getCategories().remove(existingCategory);
                }
                bookRepository.saveAll(existingCategory.getBooks());
            }

            // Adding category to new books (if any provided)
            if (!request.getBookIds().isEmpty()) {
                List<Book> newBooks = bookRepository.findAllById(request.getBookIds());

                // Validate all books exist
                if (newBooks.size() != request.getBookIds().size()) {
                    throw new ResourceNotFoundException("One or more books not found");
                }

                // Adding new links (Owning side)
                for (Book book : newBooks) {
                    if (!book.getCategories().contains(existingCategory)) {
                        book.getCategories().add(existingCategory);
                    }
                }
                bookRepository.saveAll(newBooks); // Updating owners
                existingCategory.setBooks(newBooks);
            }else{
                existingCategory.setBooks(null);
            }
        }
        Category savedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
       Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","id",categoryId));

        if (category.getBooks() != null && !category.getBooks().isEmpty()) {
            for (Book book : category.getBooks()) {
                book.getCategories().remove(category);
            }
            //saving in book side
            bookRepository.saveAll(category.getBooks());
        }
        categoryRepository.delete(category);
    }
}
