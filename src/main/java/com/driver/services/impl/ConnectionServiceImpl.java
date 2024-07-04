package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception
    {
        User user=userRepository2.findById(userId).get();
        if(user.getMaskedIp()!=null)
        {
            throw new Exception("Already connected");
        }

        if(countryName.equalsIgnoreCase(user.getOriginalCountry().getCountryName().toString()))
        {
            return user;
        }

        if (user.getServiceProviderList() == null)
        {
            throw new Exception("Unable to connect");
        }

        List<ServiceProvider> serviceProviderList=user.getServiceProviderList();
        ServiceProvider serviceProviderLowestId=null;
        int lowestId=Integer.MAX_VALUE;
        Country country=null;

        for (ServiceProvider serviceProvider:serviceProviderList)
        {
            List<Country> countryList=serviceProvider.getCountryList();
            for(Country country1:countryList)
            {
                if(countryName.equalsIgnoreCase(country1.getCountryName().toString())
                        && lowestId>serviceProvider.getId())
                {
                    lowestId=serviceProvider.getId();
                    serviceProviderLowestId=serviceProvider;
                    country=country1;
                }
            }
        }

        if (serviceProviderLowestId!=null)
        {
            Connection connection=new Connection();
            connection.setUser(user);
            connection.setServiceProvider(serviceProviderLowestId);

            user.setMaskedIp(country.getCode()+"."+serviceProviderLowestId.getId()+"."+userId);
            user.setConnected(true);
            user.getConnectionList().add(connection);

            serviceProviderLowestId.getConnectionList().add(connection);
            userRepository2.save(user);
            serviceProviderRepository2.save(serviceProviderLowestId);
        }
        else
        {
            throw new Exception("Unable to connect");
        }
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception
    {
        User user=userRepository2.findById(userId).get();
        if (!user.getConnected())
        {
            throw new Exception("Already disconnected");
        }

        user.setMaskedIp(null);
        user.setConnected(false);
        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception
    {
        //Establish a connection between sender and receiver users
        //To communicate to the receiver, sender should be in the current country of the receiver.
        //If the receiver is connected to a vpn, his current country is the one he is connected to.
        //If the receiver is not connected to vpn, his current country is his original country.
        //The sender is initially not connected to any vpn. If the sender's original country does not match receiver's current country, we need to connect the sender to a suitable vpn. If there are multiple options, connect using the service provider having smallest id
        //If the sender's original country matches receiver's current country, we do not need to do anything as they can communicate. Return the sender as it is.
        //If communication can not be established due to any reason, throw "Cannot establish communication" exception

        User sender=userRepository2.findById(senderId).get();
        User receiver=userRepository2.findById(receiverId).get();

        if(receiver.getMaskedIp()!=null)
        {
            String maskedIp=receiver.getMaskedIp();
            String code=maskedIp.substring(0,3);
            code=code.toUpperCase();
            if(code.equals(sender.getOriginalCountry().getCode()))
            {
                return sender;
            }

            String countryName="";
            CountryName[] countryNameList=CountryName.values();
            for(CountryName countryName1:countryNameList)
            {
                if(countryName1.toCode().equals(code))
                {
                    countryName=countryName1.toString();
                }
            }

            try
            {
                sender=connect(senderId,countryName);
            }
            catch (Exception e)
            {
                throw new Exception("Cannot establish communication");
            }

            if(!sender.getConnected())
            {
                throw new Exception("Cannot establish communication");
            }
            return sender;
        }
        if(sender.getOriginalCountry().equals(receiver.getOriginalCountry()))
        {
            return sender;
        }
        String countryName=receiver.getOriginalCountry().getCountryName().toString();
        try
        {
            sender = connect(senderId,countryName);
        }
        catch (Exception e)
        {
            if (!sender.getConnected())
            {
                throw new Exception("Cannot establish communication");
            }
        }
        return sender;
    }
}
