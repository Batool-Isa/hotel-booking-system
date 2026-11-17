Feature: Booking service

  Scenario: The application starts with an empty booking table
    Then the application has started with an empty booking table

  Scenario: Requesting a booking without logging in is rejected
    When I request booking with id 1 without a token
    Then the response status should be 403

  Scenario: Requesting a booking of another user is rejected
    Given a booking exists that belongs to user 1
    And I am logged in as a CUSTOMER with id 2
    When I request that booking
    Then the response status should be 403

  Scenario: Requesting one of my booking is accepted
    Given a booking exists that belongs to user 1
    And I am logged in as a CUSTOMER with id 1
    When I request that booking
    Then the response status should be 200