-- Insert test data for the Ticket entity

-- Ticket 1
INSERT INTO Ticket (id, event_id, attendee_id, bus_id, status, created_at, updated_at)
VALUES ('60c0a5a3-6cf6-4853-bec9-501b3ced8bd6',
        'e81d4bd8-ae61-4d26-9da1-4fe472f214ec',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        '0124f0cc-9d87-41e9-b2fa-b8c6c365e7dc',
        'CREATED',
        '2023-11-11 09:00:00',
        '2023-11-11 09:00:00');

-- Ticket 2
INSERT INTO Ticket (id, event_id, attendee_id, bus_id, status, created_at, updated_at)
VALUES ('f7b9b7a0-9f9d-4b9b-9b9b-5c9d9b9b9b9b',
        'e81d4bd8-ae61-4d26-9da1-4fe472f214ec',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        '0124f0cc-9d87-41e9-b2fa-b8c6c365e7dc',
        'CANCELLED',
        '2023-11-11 09:00:00',
        '2023-11-11 09:00:00');

-- Ticket 3
INSERT INTO Ticket (id, event_id, attendee_id, bus_id, status, created_at, updated_at)
VALUES ('f4b9b7a0-9f9d-4b9b-9b9b-5c9d9b9b9b9b',
        'e81d4bd8-ae61-4d26-9da1-4fe472f214ec',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        '0124f0cc-9d87-41e9-b2fa-b8c6c365e7dc',
        'PAID',
        '2023-11-11 09:00:00',
        '2023-11-11 09:00:00');

-- Ticket 4
INSERT INTO Ticket (id, event_id, attendee_id, bus_id, status, created_at, updated_at)
VALUES ('f2b9b7a0-9f9d-4b9b-9b9b-5c9d9b9b9b9b',
        'e81d4bd8-ae61-4d26-9da1-4fe472f214ec',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        '0124f0cc-9d87-41e9-b2fa-b8c6c365e7dc',
        'EXCHANGED',
        '2023-11-11 09:00:00',
        '2023-11-11 09:00:00');

-- Ticket 5
INSERT INTO Ticket (id, event_id, attendee_id, bus_id, status, created_at, updated_at)
VALUES ('f1b9b7a0-9f9d-4b9b-9b9b-5c9d9b9b9b9b',
        'e81d4bd8-ae61-4d26-9da1-4fe472f214ec',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        '0124f0cc-9d87-41e9-b2fa-b8c6c365e7dc',
        'SCANNED',
        '2023-11-11 09:00:00',
        '2023-11-11 09:00:00');