package ao.skool.app.phase0;

import ao.skool.common.domain.BilheteIdentidade;
import ao.skool.common.domain.Money;
import ao.skool.common.domain.Nif;
import ao.skool.common.domain.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 0 — the Angolan value objects every later module stores. No Spring: these are
 * pure invariants, and they should stay fast enough that nobody is tempted to skip them.
 */
class ValueObjectTest {

    @Nested
    @DisplayName("Bilhete de Identidade")
    class Bi {

        @ParameterizedTest
        @ValueSource(strings = {"001234567LA042", "123456789BG001", "999999999CN999"})
        void acceptsTheAngolanFormat(String raw) {
            assertThat(new BilheteIdentidade(raw).value()).isEqualTo(raw);
        }

        @Test
        void normalisesCaseAndSurroundingWhitespace() {
            assertThat(new BilheteIdentidade("  001234567la042 ").value()).isEqualTo("001234567LA042");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",                 // empty
                "001234567LA04",    // 2 trailing digits, needs 3
                "001234567LA0422",  // 4 trailing digits
                "00123456LA042",    // 8 leading digits, needs 9
                "001234567L042",    // 1 letter, needs 2
                "001234567LAB42",   // letter in the numeric tail
                "001234567-LA-042"  // punctuation
        })
        void rejectsMalformedValues(String raw) {
            assertThatThrownBy(() -> new BilheteIdentidade(raw))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid BI format");
        }

        @Test
        void rejectsNull() {
            assertThatThrownBy(() -> new BilheteIdentidade(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("NIF")
    class NifTest {

        @ParameterizedTest
        @ValueSource(strings = {"0012345678", "5417896321"})
        void acceptsTenDigits(String raw) {
            assertThat(new Nif(raw).value()).isEqualTo(raw);
        }

        @Test
        void trimsWhitespace() {
            assertThat(new Nif(" 0012345678 ").value()).isEqualTo("0012345678");
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "12345678901", "00123456AB", ""})
        void rejectsAnythingElse(String raw) {
            assertThatThrownBy(() -> new Nif(raw)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Money (Kwanza)")
    class MoneyTest {

        @Test
        void kwanzaScalesToTwoDecimals() {
            assertThat(Money.kwanza(15_000).amount()).isEqualByComparingTo("15000.00");
            assertThat(Money.kwanza(new BigDecimal("15000.1")).amount()).isEqualByComparingTo("15000.10");
        }

        @Test
        void roundsHalfEvenSoRepeatedBillingDoesNotDriftUpwards() {
            // Banker's rounding: .125 -> .12 (down to even), .135 -> .14 (up to even).
            assertThat(Money.kwanza(new BigDecimal("10.125")).amount()).isEqualByComparingTo("10.12");
            assertThat(Money.kwanza(new BigDecimal("10.135")).amount()).isEqualByComparingTo("10.14");
        }

        @Test
        void addsAndSubtracts() {
            Money a = Money.kwanza(30_000);
            Money b = Money.kwanza(12_500);
            assertThat(a.add(b).amount()).isEqualByComparingTo("42500.00");
            assertThat(a.subtract(b).amount()).isEqualByComparingTo("17500.00");
        }

        @Test
        void detectsNegativeAndZero() {
            assertThat(Money.kwanza(0).isZero()).isTrue();
            assertThat(Money.kwanza(0).isNegative()).isFalse();
            assertThat(Money.kwanza(1_000).subtract(Money.kwanza(2_500)).isNegative()).isTrue();
        }

        @Test
        void refusesToMixCurrencies() {
            Money kz = Money.kwanza(1_000);
            Money usd = new Money(new BigDecimal("10.00"), java.util.Currency.getInstance("USD"));
            assertThatThrownBy(() -> kz.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }

    @Nested
    @DisplayName("TenantId")
    class Tenant {

        @Test
        void parsesFromStringAndRendersBack() {
            UUID raw = UUID.randomUUID();
            assertThat(TenantId.of(raw.toString()).value()).isEqualTo(raw);
            assertThat(TenantId.of(raw)).hasToString(raw.toString());
        }

        @Test
        void equalityIsByValueSoItIsSafeAsAMapKey() {
            UUID raw = UUID.randomUUID();
            assertThat(TenantId.of(raw)).isEqualTo(TenantId.of(raw.toString()));
        }
    }
}
