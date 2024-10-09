package in.mm.main.security;

import in.mm.main.model.AppRole;
import in.mm.main.model.Role;
import in.mm.main.model.User;
import in.mm.main.repositories.RoleRepository;
import in.mm.main.repositories.UserRepository;
import in.mm.main.security.jwt.AuthEntryPointJwt;
import in.mm.main.security.jwt.AuthTokenFilter;
import in.mm.main.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
         return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") //permitAll()
                        //.requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("api/test/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web -> web.ignoring().requestMatchers("/v2/api-docs",
                "/configuration/ui",
                "/swagger-resources/**",
                "/webjars/**",
                "/configuration/security",
                "/swagger-ui.html"));
    }

    // init some user initialy
    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder){
        return args -> {
            //creating role if present get else create role in db
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseGet(() -> {
                Role newUserRole = new Role(AppRole.ROLE_USER);
                return roleRepository.save(newUserRole);
            });

            Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseGet(() -> {
                Role newSellerRole = new Role(AppRole.ROLE_SELLER);
                return roleRepository.save(newSellerRole);
            });

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseGet(() -> {
                Role newAdminRole = new Role(AppRole.ROLE_ADMIN);
                return roleRepository.save(newAdminRole);
            });

//            Set<Role> userRoles = Set.of(userRole);
//            Set<Role> sellerRoles = Set.of(sellerRole);
//            Set<Role> adminRoles = Set.of(userRole,sellerRole,adminRole);
//
//            // creating user if not exist
//            if(!userRepository.existsByUsername("user1")){
//                User user1 = new User("user1","user1@gmail.com",passwordEncoder.encode("user123"));
//                userRepository.save(user1);
//            }
//
//            if(!userRepository.existsByUsername("seller1")){
//                User seller1 = new User("seller1","seller1@gmail.com",passwordEncoder.encode("seller123"));
//                userRepository.save(seller1);
//            }
//
//            if(!userRepository.existsByUsername("admin")){
//                User admin = new User("admin","admin@gmail.com",passwordEncoder.encode("admin123"));
//                userRepository.save(admin);
//            }
//
//            // update roles for exisitng user
//            userRepository.findByUsername("user1").ifPresent(user -> {
//                user.setRoles(userRoles);
//                userRepository.save(user);
//            });
//
//            userRepository.findByUsername("seller1").ifPresent(seller -> {
//                seller.setRoles(sellerRoles);
//                userRepository.save(seller);
//            });
//
//            userRepository.findByUsername("admin").ifPresent(admin -> {
//                admin.setRoles(adminRoles);
//                userRepository.save(admin);
//            });

        };
    }

}
