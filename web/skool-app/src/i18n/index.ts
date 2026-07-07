import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import ptAO from './pt-AO.json';
import en from './en.json';

void i18n.use(initReactI18next).init({
  resources: {
    'pt-AO': { translation: ptAO },
    en: { translation: en },
  },
  lng: 'pt-AO',
  fallbackLng: 'pt-AO',
  interpolation: { escapeValue: false },
});

export default i18n;
