package com.servicehub.service;

import com.servicehub.dto.ServiceRequest;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Camada de serviço responsável pelas regras de negócio dos serviços oferecidos
 * na plataforma ServiceHub. Recurso principal do CRUD exigido pelo Checkpoint.
 */
@Service
@Transactional
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceRepository;
    private final UserRepository userRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceRepository, UserRepository userRepository) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    public ServiceOffering create(ServiceRequest request) {
        User provider = userRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prestador (usuário) não encontrado com o ID: " + request.getProviderId()));

        ServiceOffering service = new ServiceOffering();
        service.setTitle(request.getTitle());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(request.getCategory());
        service.setProvider(provider);
        service.setActive(true);
        return serviceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> findAll() {
        return serviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServiceOffering findById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com o ID: " + id));
    }

    public ServiceOffering update(Long id, ServiceRequest request) {
        ServiceOffering service = findById(id);

        User provider = userRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prestador (usuário) não encontrado com o ID: " + request.getProviderId()));

        service.setTitle(request.getTitle());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(request.getCategory());
        service.setProvider(provider);
        return serviceRepository.save(service);
    }

    public void delete(Long id) {
        ServiceOffering service = findById(id);
        serviceRepository.delete(service);
    }
}
