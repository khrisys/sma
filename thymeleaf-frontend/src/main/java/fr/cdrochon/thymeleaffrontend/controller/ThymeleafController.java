package fr.cdrochon.thymeleaffrontend.controller;

import fr.cdrochon.thymeleaffrontend.entity.Client;
import fr.cdrochon.thymeleaffrontend.entity.Garage;
import fr.cdrochon.thymeleaffrontend.repository.ClientRepository;
import fr.cdrochon.thymeleaffrontend.repository.GarageRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ThymeleafController {
    private GarageRepository garageRepository;
    private ClientRepository clientRepository;
    private ClientRegistrationRepository clientRegistrationRepository;

    public ThymeleafController(GarageRepository garageRepository, ClientRepository clientRepository, ClientRegistrationRepository clientRegistrationRepository) {
        this.garageRepository = garageRepository;
        this.clientRepository = clientRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/garage/{id}")
    @PreAuthorize("hasAuthority('USER')")
    public String garageById(@PathVariable Long id, Model model) {
        Garage garage = garageRepository.findById(id).get();
        model.addAttribute("garage", garage);
        model.addAttribute("id", garage.getId());
        return "garage"; //FIXME
    }

    /**
     * Requete vers le ms garage avec RestClient
     *
     * @param model
     * @return
     */
    @GetMapping("/garages")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String garages(Model model) {
        //FIXME Performance RestClient et jwtTokenValue!!!!
        RestClient restClient = RestClient.create("http://localhost:8081");
        List<Garage> garages =
                restClient.get().uri("/garages").headers(httpHeaders -> httpHeaders.set(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getJwtTokenValue())).retrieve().body(new ParameterizedTypeReference<List<Garage>>() {
                });
        model.addAttribute("garages", garages);
        return "garages";
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String clients(Model model) {
        List<Client> clients = clientRepository.findAll();
        model.addAttribute("clients", clients);
        return "clients";
    }

    /**
     * Path qui permet de recuperer les informations sur la session courante et les users authentifiés
     *
     * @param authentication authentication
     * @return objet authentication au format json grace à @ResponseBody
     */
    @GetMapping("/auth")
    @ResponseBody
    public Authentication authentication(Authentication authentication) {
        return authentication;
    }

    /**
     * Par defaut, l'appli s'ouvre sans path. Lorsque c'est le cas, on renseigne le path à une page index.html
     *
     * @return
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Renvoi l'user vers la page notAuthorized.html lorsqu'il tente de se rendre sur une url du site dont il n'a pas les droits
     *
     * @return page notAuthorized.html
     */
    @GetMapping("/notAutorized")
    public String notAutorized() {
        return "notAuthorized";
    }

    /**
     * Personnalisation de la page d'authentification en affichant la liste des providers, mais avec la possibilité
     * d'ajouter du css ou autre, dont images, etc.
     *
     * @param model
     * @return
     */
    @GetMapping("/oauth2Login")
    public String oauth2Login(Model model) {
        String authorizationRequestBaseUri = "oauth2/authorization";
        Map<String, String> oauth2AuthenticationUrls = new HashMap();
        Iterable<ClientRegistration> clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
        ;
        clientRegistrations.forEach(registration -> {
            oauth2AuthenticationUrls.put(registration.getClientName(),
                    authorizationRequestBaseUri + "/" + registration.getRegistrationId());
        });
        model.addAttribute("urls", oauth2AuthenticationUrls);
        return "oauth2Login";
    }

    /**
     * Recupere le token jwt de l'user qui s'est authentifié
     * <p>
     * L'objet OAuth2AuthenticationToken suppose qu'on a fait l'authentification avec un provider qui
     * supporte OpenID (keycloak ou google)
     * <p>
     * on doit importer la dependance oauth2-client pour la methode OAuth2AuhtenticationToken
     *
     * @return
     */
    private String getJwtTokenValue() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        DefaultOidcUser oidcUser = (DefaultOidcUser) oAuth2AuthenticationToken.getPrincipal();
        String jwtTokenValue = oidcUser.getIdToken().getTokenValue();
        return jwtTokenValue;
    }
}
