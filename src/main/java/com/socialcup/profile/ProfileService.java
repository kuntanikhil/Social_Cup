package com.socialcup.profile;

import com.socialcup.neighbourhood.Neighbourhood;
import com.socialcup.neighbourhood.NeighbourhoodRepository;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final NeighbourhoodRepository neighbourhoodRepository;
    private final CoffeePreferenceRepository coffeePreferenceRepository;
    private final UserCoffeePreferenceRepository userCoffeePreferenceRepository;

    public ProfileService(
            UserRepository userRepository,
            NeighbourhoodRepository neighbourhoodRepository,
            CoffeePreferenceRepository coffeePreferenceRepository,
            UserCoffeePreferenceRepository userCoffeePreferenceRepository
    ) {
        this.userRepository = userRepository;
        this.neighbourhoodRepository = neighbourhoodRepository;
        this.coffeePreferenceRepository = coffeePreferenceRepository;
        this.userCoffeePreferenceRepository = userCoffeePreferenceRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        List<CoffeePreferenceResponse> preferences = userCoffeePreferenceRepository
                .findCoffeePreferencesByUserId(userId)
                .stream()
                .map(CoffeePreferenceResponse::from)
                .toList();
        return ProfileResponse.from(user, preferences);
    }

    @Transactional(readOnly = true)
    public List<CoffeePreferenceResponse> getActiveCoffeePreferences() {
        return coffeePreferenceRepository.findByActiveTrueOrderByIdAsc()
                .stream()
                .map(CoffeePreferenceResponse::from)
                .toList();
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getUser(userId);
        Neighbourhood neighbourhood = neighbourhoodRepository
                .findByIdAndActiveTrue(request.homeNeighbourhoodId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Active neighbourhood not found"
                ));

        List<Long> requestedIds = request.coffeePreferenceIds();
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate coffee preferences are not allowed"
            );
        }

        List<CoffeePreference> activePreferences = coffeePreferenceRepository
                .findByIdInAndActiveTrue(requestedIds);
        if (activePreferences.size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more coffee preferences are invalid or inactive"
            );
        }

        Map<Long, CoffeePreference> preferencesById = activePreferences.stream()
                .collect(Collectors.toMap(CoffeePreference::getId, Function.identity()));
        List<CoffeePreference> orderedPreferences = requestedIds.stream()
                .map(preferencesById::get)
                .toList();

        user.updateProfile(request.displayName().trim(), neighbourhood);
        userCoffeePreferenceRepository.deleteAllByUserId(userId);

        List<UserCoffeePreference> replacements = new ArrayList<>(orderedPreferences.size());
        for (CoffeePreference preference : orderedPreferences) {
            replacements.add(new UserCoffeePreference(user, preference));
        }
        userCoffeePreferenceRepository.saveAll(replacements);

        return ProfileResponse.from(
                user,
                orderedPreferences.stream()
                        .map(CoffeePreferenceResponse::from)
                        .toList()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }
}
