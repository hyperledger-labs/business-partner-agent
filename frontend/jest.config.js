module.exports = {
  preset: "@vue/cli-plugin-unit-jest",
  verbose: true,
  moduleFileExtensions: [
    "js",
    "json",
    // tell Jest to handle `*.vue` files
    "vue",
  ],
  transform: {
    // process `*.vue` files with `vue-jest`
    //".*\\.(vue)$": "vue-jest"
    "^.+\\.vue$": "vue-jest",
  },
  collectCoverage: true,
  collectCoverageFrom: ["**/*.{js,vue}", "!**/node_modules/**"],
  //testMatch: ['<rootDir>/(src/**/*.spec.(ts|tsx|js)|**/__tests__/*.(ts|tsx|js))']
  testMatch: ["<rootDir>/src/**/*.(spec|test).(ts|js)"],
};
