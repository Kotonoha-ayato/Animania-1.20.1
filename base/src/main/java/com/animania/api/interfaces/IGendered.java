package com.animania.api.interfaces;
import com.animania.api.data.AnimalGender;
public interface IGendered extends IAnimaniaAnimal { default AnimalGender getModernGender() { return getGender(); } }
