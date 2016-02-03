package com.eigenmusik.sources;

import com.eigenmusik.exceptions.SourceAuthenticationException;
import com.eigenmusik.tracks.TrackService;
import com.eigenmusik.user.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SourceService {

    private final SourceAccountRepository sourceAccountRepository;
    private final SourceFactory sourceFactory;
    private final TrackService trackService;

    @Autowired
    public SourceService(SourceAccountRepository sourceAccountRepository, SourceFactory sourceFactory, TrackService trackService) {
        this.sourceAccountRepository = sourceAccountRepository;
        this.sourceFactory = sourceFactory;
        this.trackService = trackService;
    }

    public List<SourceAccount> getAccounts(UserProfile userProfile) {
        return sourceAccountRepository.findByOwner(userProfile);
    }

    public SourceAccount addAccount(SourceType sourceType, String code, UserProfile userProfile) throws SourceAuthenticationException {
        Source source = sourceFactory.build(sourceType);

        SourceAccount sourceAccount = source.getAccount(code);
        sourceAccount.setOwner(userProfile);
        source.save(sourceAccount);

        syncAccount(sourceAccount);

        return sourceAccount;
    }

    public void syncAccount(SourceAccount sourceAccount) {
        trackService.save(sourceFactory.build(sourceAccount.getSource()).getTracks(sourceAccount), sourceAccount.getOwner());
    }

    public List<SourceJson> getSources() {
        return Arrays.asList(SourceType.values())
                .stream()
                .map(sourceTypes -> sourceFactory.build(sourceTypes).getJson())
                .collect(Collectors.toList());
    }

}