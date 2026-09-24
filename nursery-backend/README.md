# Fernwick — Nursery shop (Java servlet backend + frontend)

A full working backend for the Fernwick plant-shop template: accounts, a real
basket, checkout and order history, all backed by MySQL, plus the fixes
needed on the frontend so the existing HTML/CSS actually talks to it.

## What was added / fixed

**Backend (new)** — `src/main/java/com/nursery/`
- `DBConnection` — with no configuration, connects to an embedded in-memory H2 database (zero install — great for trying the project or local dev; data resets on restart). Set `DB_URL` / `DB_USER` / `DB_PASSWORD` to switch to MySQL for anything beyond local testing; the driver is picked automatically from the URL prefix.
- `DBInitializer` — runs on startup, creates the tables and seeds the 10 products automatically (`src/main/resources/schema.sql`), on either database. No manual migration step.
- `PasswordUtil` — salted SHA-256 password hashing.
- `RegisterServlet` (`POST /register`), `LoginServlet` (`POST /login`), `LogoutServlet` (`/logout`), `CurrentUserServlet` (`GET /current-user`) — session-based auth.
- `ProductServlet` (`GET /products`, `GET /products?id=slug`).
- `CartServlet` (`GET/POST/DELETE /cart`) — cart is tied to the signed-in session, not a client-supplied id; the add/increment logic is plain SQL (no vendor-specific clause) so it works the same on H2 and MySQL.
- `OrderServlet` (`POST /order` places the order transactionally: stock check → order → order_items → decrement stock → clear cart, all-or-nothing; `GET /order` returns order history).

**Frontend (fixed)** — the uploaded `main.js` called endpoints that didn't
exist and listened for `data-*` attributes the HTML doesn't actually use
(`[data-add-to-cart]` vs. the template's real `[data-add]`, `[data-category]`
vs. `[data-tags]`, `[data-cart-items]` vs. `[data-cart-lines]`, etc.), so
nothing was wired up. `main.js` was rewritten to match the template's real
markup and the new API:
- Add-to-basket, basket drawer/page, remove item, live subtotal/delivery/total.
- Real checkout button → `POST /order` → redirects to order history.
- Sign in / register / sign out, with the header account icon and a new
  "Orders" nav link reflecting the session.
- Product detail page (`product.html?id=<slug>`) now loads real data (price,
  stock, badge, description) from `/products` instead of being hardcoded to
  Monstera; every product card across the site now links to its own page.
- Shop page filters (chips), sort, live search, gallery, quantity stepper,
  wishlist toggle and the newsletter/contact "demo" forms all now work.
- Two new pages: `login.html`, `register.html`, and `orders.html` (order history).

## Project layout

```
pom.xml
Dockerfile
src/main/java/com/nursery/        servlets + helpers
src/main/resources/schema.sql     tables + seed data (auto-run on startup)
src/main/webapp/                  all the HTML/CSS/JS (served at "/")
```

## Run it locally (zero setup — embedded H2)

No database install needed. Just build and deploy:

```
mvn clean package
# deploy target/nursery.war to Tomcat 10.1+, e.g. copy it to webapps/ROOT.war
```

On first request, tables are created and the 10 products are seeded into
an in-memory H2 database automatically. Sign up, add things to your basket,
check out — it all works. The only catch: data resets whenever the app
restarts, since it's in memory. That's fine for trying it out or developing;
switch to MySQL (below) for anything that needs to persist.

## Run it with MySQL (recommended for real use / deployment)

1. Create an empty database:
   ```sql
   CREATE DATABASE nursery;
   ```
2. Set the connection env vars before starting the app:
   ```
   export DB_URL="jdbc:mysql://localhost:3306/nursery?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   export DB_USER=root
   export DB_PASSWORD=yourpassword
   ```
3. Build and deploy as above — `DBConnection` detects the `jdbc:mysql:` prefix
   and switches drivers automatically. Tables + seed data are created the
   same way, this time persisted in MySQL.

## Run it with Docker

```
docker build -t fernwick .
docker run -p 10000:10000 \
  -e DB_URL="jdbc:mysql://<host>:3306/nursery?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  -e DB_USER=root \
  -e DB_PASSWORD=yourpassword \
  fernwick
```

On Render (or similar), set `DB_URL` / `DB_USER` / `DB_PASSWORD` as
environment variables pointing at your managed MySQL instance — the
Dockerfile already respects `$PORT`.

## API summary

| Method | Path            | Body / query               | Notes                                  |
|--------|-----------------|-----------------------------|-----------------------------------------|
| POST   | `/register`     | `name, email, password`     | Creates account, starts session         |
| POST   | `/login`        | `email, password`           | Starts session                          |
| GET/POST | `/logout`     | –                            | Ends session                            |
| GET    | `/current-user` | –                            | `{loggedIn, id, name, email}`           |
| GET    | `/products`     | `?id=slug` optional          | All products, or one product            |
| GET    | `/cart`         | –                            | Signed-in user's basket                 |
| POST   | `/cart`         | `product_id, quantity`      | Add / increment                         |
| DELETE | `/cart`         | `?id=cartRowId`              | Remove a line                           |
| POST   | `/order`        | –                            | Places order from the current basket    |
| GET    | `/order`        | –                            | Order history for the signed-in user    |

All cart/order endpoints trust the server-side session, not any user id
sent by the browser.

## Notes

- Pot-size "variants" on the product page are still cosmetic (the DB has
  one price per product) — the price shown updates, but there's a single
  SKU per plant, same as before.
- No payment provider is wired in; checkout creates a `Pending` order.
- The compiled `OrderServlet.class` that was originally uploaded has been
  replaced by a matching `.java` source file so the whole backend can be
  rebuilt from source.
