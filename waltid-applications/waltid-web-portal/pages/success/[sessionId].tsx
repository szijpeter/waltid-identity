import WaltIcon from "@/components/walt/logo/WaltIcon";
import {CheckCircleIcon} from "@heroicons/react/24/outline";
import {useContext, useEffect, useMemo, useState} from "react";
import {useRouter} from "next/router";
import axios from "axios";
import nextConfig from "@/next.config";
import Modal from "@/components/walt/modal/BaseModal";
import {EnvContext} from "@/pages/_app";

type DisplayPolicyEntry = {
  policy: string;
  is_success: boolean;
};

type DisplayPolicyGroup = {
  policyResults: DisplayPolicyEntry[];
};

type DisplayCredential = {
  type?: string[] | string;
  vct?: string;
  credentialSubject?: Record<string, unknown>;
  [key: string]: unknown;
};

const EMPTY_POLICY_GROUP: DisplayPolicyGroup = { policyResults: [] };

export default function Success() {
  const env = useContext(EnvContext);
  const router = useRouter();
  const isVerifier2Engine = router.query.engine?.toString() === 'verifier2';
  const [vctName, setVctName] = useState<string | null>(null);

  const [policyResults, setPolicyResults] = useState<DisplayPolicyGroup[]>([]);
  const [credentials, setCredentials] = useState<DisplayCredential[]>([]);
  const [index, setIndex] = useState<number>(0);
  const [modal, setModal] = useState<boolean>(false);
  const [statusLabel, setStatusLabel] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);

  useEffect(() => {
    if (!router.isReady || !router.query.sessionId) return;

    if (isVerifier2Engine) {
      const verifier2BaseUrl = env.NEXT_PUBLIC_VERIFIER2
        ? env.NEXT_PUBLIC_VERIFIER2
        : nextConfig.publicRuntimeConfig!.NEXT_PUBLIC_VERIFIER2;
      axios
        .get(
          `${verifier2BaseUrl}/verification-session/${encodeURIComponent(router.query.sessionId.toString())}/info`
        )
        .then((response) => {
          const sessionInfo = response.data as Record<string, unknown>;
          const rawPresentedCredentials = sessionInfo.presented_credentials
            ?? sessionInfo.presentedCredentials
            ?? sessionInfo.presented_presentations
            ?? asRecord(sessionInfo.presented_raw_data)?.vpToken
            ?? asRecord(sessionInfo.tokenResponse)?.vp_token;
          const rawPolicyResults = sessionInfo.policy_results
            ?? sessionInfo.policyResults
            ?? asRecord(sessionInfo.authorizationRequest)?.policies;

          setCredentials(normalizePresentedCredentials(rawPresentedCredentials));
          setPolicyResults(normalizePolicyResults(rawPolicyResults));
          setStatusLabel(typeof sessionInfo.status === "string" ? sessionInfo.status : "Unknown");
          setPageError(null);
          setVctName(null);
        })
        .catch((error) => {
          const message = error?.response?.data?.errorDescription
            || error?.response?.data?.message
            || error?.message
            || 'Could not load verifier2 session.';
          setPageError(message);
          console.error(error);
        });
      return;
    }

    axios
      .get(
        `${env.NEXT_PUBLIC_VERIFIER ? env.NEXT_PUBLIC_VERIFIER : nextConfig.publicRuntimeConfig!.NEXT_PUBLIC_VERIFIER}/openid4vc/session/${router.query.sessionId}`
      )
      .then((response) => {
        let parsedToken = parseJwt(response.data.tokenResponse.vp_token);
        const parsedVp = asRecord(parsedToken.vp);
        let containsVP = !!parsedVp?.verifiableCredential;
        let vcs = containsVP
          ? parsedVp?.verifiableCredential
          : [response.data.tokenResponse.vp_token];

        setCredentials(normalizePresentedCredentials(vcs));

        setPolicyResults(() => {
          if (containsVP) {
            return response.data.policyResults.results;
          } else {
            //add a new entry to the policy results, since its start index +1
            return [
              {
                policyResults: [
                  {
                    policy: 'New Policy',
                    is_success: true,
                  },
                ],
              },
              ...response.data.policyResults.results,
            ];
          }
        });

        if (!containsVP) {
          const vct = parsedToken['vct'];
          if (typeof vct === "string") {
            const vctUrl = new URL(vct);
            const vctResolutionUrl = `${vctUrl.origin}/.well-known/vct${vctUrl.pathname}`;
            fetchVctName(vctResolutionUrl).then((name) => setVctName(name));
          }
        }
        setStatusLabel(null);
        setPageError(null);
      })
      .catch((error) => {
        const message = error?.response?.data?.errorDescription
          || error?.response?.data?.message
          || error?.message
          || 'Could not load verification session.';
        setPageError(message);
        console.error(error);
      });
  }, [router.isReady, router.query.sessionId, isVerifier2Engine, env]);

  useEffect(() => {
    if (index < credentials.length) {
      return;
    }
    setIndex(0);
  }, [credentials.length, index]);

  const activeCredential = credentials[index];
  const credentialRows = useMemo(
    () => buildCredentialRows(activeCredential),
    [activeCredential],
  );
  const activePolicyResults = policyResults[index + 1]?.policyResults
    ?? policyResults[index]?.policyResults
    ?? [];
  const titleLabel = getCredentialTitle(activeCredential, vctName);

  return (
    <div className="min-h-screen flex justify-center bg-gray-50 py-8 px-4 overflow-y-auto">
      <Modal show={modal} securedByWalt={false} onClose={() => setModal(false)}>
        <div className="flex flex-col items-center">
          <div className="w-full">
            <textarea
              value={JSON.stringify(
                activeCredential?.credentialSubject ?? activeCredential,
                null,
                4
              )}
              disabled={true}
              className="w-full h-48 border-2 border-gray-300 rounded-md px-2"
            />
          </div>
        </div>
      </Modal>
      <div className="relative w-full sm:w-10/12 md:w-8/12 lg:w-6/12 text-center shadow-2xl rounded-lg pt-8 pb-8 px-6 sm:px-10 bg-white">
        <h1 className="text-3xl text-gray-900 text-center font-bold mb-10">
          Presented Credentials
        </h1>
        {isVerifier2Engine && (
          <div className="mb-8 text-sm text-gray-600">
            <div>
              Session ID: <span className="font-mono text-gray-800">{router.query.sessionId?.toString()}</span>
            </div>
            <div className="mt-2">
              Status: <span className="font-semibold text-gray-800">{statusLabel ?? "Unknown"}</span>
            </div>
          </div>
        )}
        {pageError && (
          <p className="text-sm text-red-600 break-all mb-6">{pageError}</p>
        )}
        <div className="flex items-center justify-center">
          {index !== 0 && credentials.length > 1 && (
            <button
              onClick={() => setIndex(index - 1)}
              className="text-gray-500 hover:text-gray-900 focus:outline-none absolute left-10"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6 mr-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 19l-7-7 7-7"
                />
              </svg>
            </button>
          )}
          <div className={`group h-[225px] w-[400px] [perspective:1000px] ${credentials.length === 0 ? "hidden" : ""}`}>
            <div className="relative h-full w-full rounded-xl shadow-xl transition-all duration-500 [transform-style:preserve-3d] group-hover:[transform:rotateY(180deg)]">
              <div className="absolute inset-0">
                <div className="flex h-full w-full flex-col drop-shadow-sm rounded-xl py-7 px-8 text-gray-100 cursor-pointer overflow-hidden bg-gradient-to-r from-green-700 to-green-900 z-[-2]">
                  <div className="flex flex-row">
                    <WaltIcon height={35} width={35} outline type="white" />
                  </div>
                  <div className="mb-8 mt-12">
                    <h6 className={'text-2xl font-bold overflow-hidden text-ellipsis whitespace-nowrap'}>
                      {titleLabel}
                    </h6>
                  </div>
                </div>
              </div>
              <div className="absolute inset-0 h-full w-full rounded-xl bg-white p-5 text-slate-200 [transform:rotateY(180deg)] [backface-visibility:hidden] overflow-y-scroll">
                {credentialRows.map((item) => {
                  return (
                    <div key={item.key} className="flex flex-row py-1">
                      <div className="text-gray-600 text-left w-1/2 capitalize leading-[1.1]">
                        {item.key}
                      </div>
                      <div className="text-slate-800 text-left w-1/2 text-[#313233]">
                        {item.value}
                      </div>
                    </div>
                  );
                })}
                <div className="flex flex-row py-1">
                  <button
                    onClick={() => setModal(true)}
                    className="text-gray-500 text-center w-full capitalize leading-[1.1] underline"
                    disabled={!activeCredential}
                  >
                    View Credential In JSON
                  </button>
                </div>
              </div>
            </div>
          </div>
          {credentials.length === 0 && (
            <div className="rounded-xl border border-gray-200 w-full max-w-[400px] px-6 py-10 text-gray-500 text-sm">
              No presented credential payload is available for this session.
            </div>
          )}
          {index !== credentials.length - 1 && credentials.length > 1 && (
            <button
              onClick={() => setIndex(index + 1)}
              className="text-gray-500 hover:text-gray-900 focus:outline-none absolute right-10"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6 ml-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          )}
        </div>
        <div className="mt-10 px-12">
          <div className="flex flex-row items-center justify-center mb-5 text-gray-500">
            {activePolicyResults.length
              ? 'The VP was verified along with:'
              : 'The VP was not verified against any policies'}
          </div>
          <div className="xs:grid xs:grid-cols-2 items-center justify-center">
            {activePolicyResults
              .map((policy) => {
                return {
                  name:
                    policy.policy.charAt(0).toUpperCase() +
                    policy.policy.slice(1) +
                    ' Policy',
                  is_success: policy.is_success,
                };
              })
              .map((policy, idx) => {
                return (
                  <div
                    key={policy.name}
                    className={`flex items-center gap-3 overflow-hidden text-ellipsis whitespace-nowrap ${idx % 2 == 1 ? 'sm:justify-self-end' : ''}`}
                  >
                    {policy.is_success ? (
                      <CheckCircleIcon className="h-4 text-green-600" />
                    ) : (
                      <CheckCircleIcon className="h-4 text-red-600" />
                    )}
                    <div>{policy.name}</div>
                  </div>
                );
              })}
          </div>
        </div>
        <div className="flex flex-col items-center mt-12">
          <div className="flex flex-row gap-2 items-center content-center text-sm text-center text-gray-500">
            <p className="">Secured by walt.id</p>
            <WaltIcon height={15} width={15} type="gray" />
          </div>
        </div>
      </div>
    </div>
  );
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  return Buffer.from(padded, "base64").toString();
}

function parseJwt(token: string): Record<string, unknown> {
  try {
    const payload = token.split(".")[1];
    if (!payload) {
      return {};
    }
    return JSON.parse(decodeBase64Url(payload)) as Record<string, unknown>;
  } catch (error) {
    console.error("Could not parse JWT payload", error);
    return {};
  }
}

const fetchVctName = async (vctUrl: string) => {
  try {
    const response = await axios.get(vctUrl);
    const bodyJson = response.data;
    return bodyJson['name'] as string;
  } catch (error) {
    console.error('Error fetching vct:', error);
    return 'Unknown VCT';
  }
};

function toDisplayCredential(value: unknown): DisplayCredential | null {
  const record = asRecord(value);
  if (!record) {
    return null;
  }

  const vc = asRecord(record.vc);
  if (vc) {
    return toDisplayCredential(vc);
  }

  const credentialSubject = asRecord(record.credentialSubject) ?? asRecord(record.claims);
  const hasCredentialMarkers = Boolean(
    credentialSubject ||
    record.type ||
    record.vct ||
    record.id ||
    record.format
  );
  if (!hasCredentialMarkers) {
    return null;
  }

  const credential: DisplayCredential = {
    ...record,
    ...(credentialSubject ? { credentialSubject } : {}),
  };

  if (Array.isArray(record.type) || typeof record.type === "string") {
    credential.type = record.type as string[] | string;
  }
  if (typeof record.vct === "string") {
    credential.vct = record.vct;
  }

  return credential;
}

function parseCredentialToken(vcToken: string): DisplayCredential | null {
  if (!vcToken || typeof vcToken !== "string") {
    return null;
  }

  const split = vcToken.split("~");
  const parsed = parseJwt(split[0]);

  if (split.length === 1) {
    return toDisplayCredential(parsed) ?? toDisplayCredential(parsed.vc) ?? null;
  }

  const credentialWithSdJWTAttributes = (toDisplayCredential(parsed) ?? {}) as DisplayCredential;
  const parsedVc = asRecord(parsed.vc);
  split.slice(1).forEach((item) => {
    if (item.split('.').length === 3) {
      return;
    }

    try {
      const parsedItem = JSON.parse(decodeBase64Url(item)) as unknown;
      if (!Array.isArray(parsedItem) || parsedItem.length < 3 || typeof parsedItem[1] !== "string") {
        return;
      }

      const existingCredentialSubject = asRecord(credentialWithSdJWTAttributes.credentialSubject) ?? {};
      credentialWithSdJWTAttributes.credentialSubject = {
        [parsedItem[1]]: parsedItem[2],
        ...existingCredentialSubject,
      };
    } catch (error) {
      console.error("Could not decode SD-JWT disclosure", error);
    }
  });

  if (Array.isArray(parsedVc?.type) || typeof parsedVc?.type === "string") {
    credentialWithSdJWTAttributes.type = parsedVc?.type as string[] | string;
  }

  return credentialWithSdJWTAttributes;
}

function normalizePresentedCredentials(raw: unknown): DisplayCredential[] {
  if (raw == null) {
    return [];
  }

  if (Array.isArray(raw)) {
    return raw.flatMap((item) => normalizePresentedCredentials(item));
  }

  if (typeof raw === "string") {
    const parsed = parseCredentialToken(raw);
    return parsed ? [parsed] : [];
  }

  const record = asRecord(raw);
  if (!record) {
    return [];
  }

  const vp = asRecord(record.vp);
  if (vp?.verifiableCredential) {
    return normalizePresentedCredentials(vp.verifiableCredential);
  }

  if (record.vp_token) {
    return normalizePresentedCredentials(record.vp_token);
  }
  if (record.vpToken) {
    return normalizePresentedCredentials(record.vpToken);
  }

  const credential = toDisplayCredential(record);
  return credential ? [credential] : [];
}

function normalizePolicyResults(raw: unknown): DisplayPolicyGroup[] {
  if (!raw) {
    return [EMPTY_POLICY_GROUP];
  }

  const policyRecord = asRecord(raw);
  if (policyRecord?.results) {
    return normalizePolicyResults(policyRecord.results);
  }
  if (policyRecord?.policyResults) {
    return normalizePolicyResults(policyRecord.policyResults);
  }
  if (policyRecord?.policy_results) {
    return normalizePolicyResults(policyRecord.policy_results);
  }

  const rawPolicies = Array.isArray(raw) ? raw : [raw];
  const normalizedPolicies = rawPolicies
    .map((policy) => {
      const policyObj = asRecord(policy);
      if (!policyObj) {
        return null;
      }

      const policyNameRaw = policyObj.policy ?? policyObj.name ?? policyObj.id;
      const policyName = typeof policyNameRaw === "string" ? policyNameRaw : "Policy";
      const isSuccessRaw = policyObj.is_success ?? policyObj.isSuccess ?? policyObj.result;
      const isSuccess = typeof isSuccessRaw === "boolean" ? isSuccessRaw : Boolean(isSuccessRaw);

      return {
        policy: policyName,
        is_success: isSuccess,
      };
    })
    .filter((policy): policy is DisplayPolicyEntry => policy !== null);

  if (!normalizedPolicies.length) {
    return [EMPTY_POLICY_GROUP];
  }

  return [EMPTY_POLICY_GROUP, { policyResults: normalizedPolicies }];
}

function getCredentialTitle(credential: DisplayCredential | undefined, vctName: string | null): string {
  if (!credential) {
    return "Credential";
  }

  if (Array.isArray(credential.type) && credential.type.length > 0) {
    return credential.type[credential.type.length - 1].replace(/([a-z0-9])([A-Z])/g, "$1 $2");
  }
  if (typeof credential.type === "string" && credential.type.length > 0) {
    return credential.type.replace(/([a-z0-9])([A-Z])/g, "$1 $2");
  }
  if (credential.vct) {
    return vctName ?? credential.vct;
  }

  return "Credential";
}

function buildCredentialRows(credential: DisplayCredential | undefined): Array<{ key: string; value: string }> {
  if (!credential) {
    return [];
  }

  const sourceRecord = asRecord(credential.credentialSubject) ?? asRecord(credential);
  if (!sourceRecord) {
    return [];
  }

  return Object.entries(sourceRecord)
    .map(([key, value]) => {
      if (typeof value !== "string" || value.length === 0 || value.length >= 40) {
        return null;
      }
      return {
        key: (key.charAt(0).toUpperCase() + key.slice(1)).replace(/([a-z0-9])([A-Z])/g, "$1 $2"),
        value,
      };
    })
    .filter((item): item is { key: string; value: string } => item !== null);
}
