package in.mm.main.service;

import in.mm.main.exceptions.ResourceNotFoundException;
import in.mm.main.model.Address;
import in.mm.main.model.User;
import in.mm.main.payload.AddressDTO;
import in.mm.main.repositories.AddressRepository;
import in.mm.main.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService{

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

//        List<Address> addressList = user.getAddresses();
//        addressList.add(address);
//        user.setAddresses(addressList);

        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAllAddresses() {
        List<Address> savedAddresses = addressRepository.findAll();
        List<AddressDTO> addressDTOList = savedAddresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOList;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address savedAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> savedAddresses = user.getAddresses();
        List<AddressDTO> addressDTOList = savedAddresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOList;
    }

    @Transactional
    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDb = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

        addressFromDb.setStreet(addressDTO.getStreet());
        addressFromDb.setCity(addressDTO.getCity());
        addressFromDb.setState(addressDTO.getState());
        addressFromDb.setCountry(addressDTO.getCountry());
        addressFromDb.setPincode(addressDTO.getPincode());
        addressFromDb.setBuildingName(addressDTO.getBuildingName());
        Address addressUpdated = addressRepository.save(addressFromDb);

//        User user = addressFromDb.getUser();
//        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
//        user.getAddresses().add(addressUpdated);
//        userRepository.save(user);

        return modelMapper.map(addressUpdated, AddressDTO.class);
    }

    @Transactional
    @Override
    public String deleteAddressById(Long addressId) {
        Address addressFromDb = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

//        User user = addressFromDb.getUser();
//        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
//        userRepository.save(user);
        addressRepository.delete(addressFromDb);
        return "Address with addressId "+addressId+" Deleted Successfully";
    }
}
