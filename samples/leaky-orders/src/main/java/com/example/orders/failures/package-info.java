/**
 * Startup failures on purpose, one Spring profile each, so that jdoctor can be tried on real logs.
 *
 * <ul>
 *   <li>{@code duplicate-bean}: two configurations define a bean with the same name.</li>
 *   <li>{@code two-candidates}: two beans of one type, injected without a qualifier.</li>
 *   <li>{@code missing-bean}: a dependency with no implementation.</li>
 *   <li>{@code cycle}: two services that need each other through their constructors.</li>
 *   <li>{@code init-crash}: a NullPointerException in a {@code @PostConstruct} method.</li>
 * </ul>
 */
package com.example.orders.failures;
