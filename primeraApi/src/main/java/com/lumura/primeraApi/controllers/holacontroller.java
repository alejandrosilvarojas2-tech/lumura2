package com.lumura.primeraApi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
    @RestController
    @RequestMapping("/apisaludos")
public class holacontroller {
    @GetMapping ("/hola")
    String hola(){
        return "hola carechimba";
    }
    @GetMapping("/holanombre/{nombre}/{edad}")
    public String holaNombre (@PathVariable String nombre, @PathVariable int edad)
    {
        return "hola" + nombre + "tu edad es:" + edad;

    }


}