package com.technicalblog.entity;

/**
 * Application roles. Version 1 only issues ADMIN accounts through the seeder,
 * USER exists so the model does not have to change when public accounts arrive.
 */
public enum Role {
    ADMIN,
    USER
}
