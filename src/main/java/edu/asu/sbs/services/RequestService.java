package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.repositories.RequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RequestService {

    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public List<Request> getAllRequests() {
        List<Request> requestList = Lists.newArrayList();
        requestRepository.findAll().forEach(requestList::add);
        return requestList;
    }

    public Optional<Request> getRequest(Long id) {
        return requestRepository.findOneByRequestId(id);
    }
}
