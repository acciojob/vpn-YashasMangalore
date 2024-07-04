package com.driver.services.impl;

import com.driver.model.Country;
import com.driver.model.CountryName;
import com.driver.model.ServiceProvider;
import com.driver.model.User;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository3;
    @Autowired
    ServiceProviderRepository serviceProviderRepository3;
    @Autowired
    CountryRepository countryRepository3;

    @Override
    public User register(String username, String password, String countryName) throws Exception
    {
        String countryNameUpper=countryName.toUpperCase();
        if (!countryNameUpper.equals("IND") && !countryNameUpper.equals("USA") && !countryNameUpper.equals("CHI") && !countryNameUpper.equals("JPN"))
        {
            throw new Exception("Country not found");
        }
        Country country=new Country(CountryName.valueOf(countryName),CountryName.valueOf(countryName).toCode());
        //create user
        User user=new User();
        user.setPassword(password);
        user.setUsername(username);
        user.setConnected(false);

        //add user to country
        country.setUser(user);
        user.setOriginalCountry(country);
        //save user
        user=userRepository3.save(user);

        user.setOriginalIp(user.getOriginalCountry().getCode() +"."+user.getId());
        user=userRepository3.save(user);
        return user;
    }

    @Override
    public User subscribe(Integer userId, Integer serviceProviderId)
    {
        User user=userRepository3.findById(userId).get();
        ServiceProvider serviceProvider=serviceProviderRepository3.findById(serviceProviderId).get();

        user.getServiceProviderList().add(serviceProvider);
        serviceProvider.getUsers().add(user);
        serviceProviderRepository3.save(serviceProvider);
        return user;
    }
}
