package kz.iqadam.esyllabus.security;

import java.util.LinkedHashSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserAccessService userAccessService;

    public MicrosoftOAuth2UserService(UserAccessService userAccessService) {
        this.userAccessService = userAccessService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var oauth2User = delegate.loadUser(userRequest);
        var authenticatedUser = userAccessService.authorize(oauth2User.getAttributes());

        var authorities = new LinkedHashSet<GrantedAuthority>(oauth2User.getAuthorities());
        authenticatedUser.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "preferred_username");
    }
}
