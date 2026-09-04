INSERT INTO coffee_preferences (code, display_name, active)
VALUES
    ('MATCHA', 'Matcha', TRUE),
    ('ESPRESSO', 'Espresso', TRUE),
    ('COLD_BREW', 'Cold Brew', TRUE),
    ('LATTE', 'Latte', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform_settings (setting_key, setting_value)
VALUES ('CREDIT_VALUE', '1.00')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO neighbourhoods (name, active, sort_order)
VALUES
    ('Uptown', TRUE, 10),
    ('Deep Ellum', TRUE, 20),
    ('Bishop Arts District', TRUE, 30),
    ('Downtown', TRUE, 40),
    ('Lower Greenville', TRUE, 50)
ON CONFLICT (name) DO NOTHING;
