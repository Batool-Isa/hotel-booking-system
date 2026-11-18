Feature: Billing service

  Scenario: A customer pays for their own pending booking
    Given a pending booking 10 that belongs to user 1
    And I am logged in as a CUSTOMER with id 1
    When I pay for booking 10 with idempotency key "key-1"
    Then the response status should be 200
    And the response contains the Stripe checkout link
    And 1 pending payment is saved for booking 10

  Scenario: A customer cannot pay for the booking of another user
    Given a pending booking 11 that belongs to user 1
    And I am logged in as a CUSTOMER with id 2
    When I pay for booking 11 with idempotency key "key-2"
    Then the response status should be 403
    And no payment is saved

  Scenario: Paying twice with the same idempotency key creates one payment
    Given a pending booking 12 that belongs to user 1
    And I am logged in as a CUSTOMER with id 1
    When I pay for booking 12 with idempotency key "key-3"
    And I pay for booking 12 with idempotency key "key-3"
    Then the response status should be 200
    And 1 pending payment is saved for booking 12
    And Stripe was asked to create only one checkout session

  Scenario: A webhook with a wrong signature is rejected
    When Stripe sends a completed payment with a wrong signature
    Then the response status should be 400

  Scenario: A signed webhook confirms the payment and the booking
    Given a pending payment for booking 14 with session "cs_test_14"
    When Stripe sends a signed completed payment for session "cs_test_14"
    Then the response status should be 200
    And the payment for booking 14 is marked as succeeded
    And the booking service was asked to confirm booking 14 once
