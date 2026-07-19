export const queryKeys = {
  companyProfile: {
    all: ['companyProfile'] as const,

    current: () =>
      [...queryKeys.companyProfile.all, 'current'] as const,
  },
} as const